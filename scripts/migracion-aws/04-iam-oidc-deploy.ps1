# ──────────────────────────────────────────────────────────────────────────
# 04 - Proveedor OIDC de GitHub + rol de deploy en la cuenta NUEVA
#
# Replica de la cuenta vieja:
#   - OIDC provider: token.actions.githubusercontent.com (aud sts.amazonaws.com)
#   - Rol: GitHubActions-IndustrialSafety
#       trust -> repo:Industrial-Safety/industrial-security-backend:*
#       inline ECS-ECR-Deploy + ECS-PassRole (account id ajustado a la cuenta nueva)
#
# Uso:  pwsh scripts/migracion-aws/04-iam-oidc-deploy.ps1
# Idempotente: salta provider/rol si ya existen.
#
# OJO: las politicas PassRole referencian ecsTaskExecutionRole y ecs-task-role
#      de la cuenta nueva. Esos roles se crean en el paso de ECS (no aqui).
# ──────────────────────────────────────────────────────────────────────────
$ErrorActionPreference = "Stop"
$PROFILE_AWS = "nueva"
$ROLE_NAME   = "GitHubActions-IndustrialSafety"
$REPO        = "repo:Industrial-Safety/industrial-security-backend:*"

# Account id de la cuenta nueva
$ACCT = aws sts get-caller-identity --profile $PROFILE_AWS --query Account --output text
$PROVIDER_ARN = "arn:aws:iam::${ACCT}:oidc-provider/token.actions.githubusercontent.com"
Write-Host "Cuenta nueva: $ACCT"

# ── 1. Proveedor OIDC de GitHub ──────────────────────────────────────────
$provExists = aws iam list-open-id-connect-providers `
    --profile $PROFILE_AWS --query "OpenIDConnectProviderList[?Arn=='$PROVIDER_ARN'].Arn" --output text
if ($provExists) {
    Write-Host "[=] OIDC provider ya existe."
} else {
    aws iam create-open-id-connect-provider `
        --url "https://token.actions.githubusercontent.com" `
        --client-id-list "sts.amazonaws.com" `
        --thumbprint-list "1c58a3a8518e8759bf075b76b750d4f2df264fcd" `
        --profile $PROFILE_AWS | Out-Null
    Write-Host "[+] OIDC provider creado."
}

# ── 2. Documentos de politica (escritos a archivos temporales) ────────────
$trust = @"
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": { "Federated": "$PROVIDER_ARN" },
    "Action": "sts:AssumeRoleWithWebIdentity",
    "Condition": {
      "StringEquals": { "token.actions.githubusercontent.com:aud": "sts.amazonaws.com" },
      "StringLike":   { "token.actions.githubusercontent.com:sub": "$REPO" }
    }
  }]
}
"@
$polEcrDeploy = @"
{
  "Version": "2012-10-17",
  "Statement": [
    { "Sid": "ECRPush", "Effect": "Allow",
      "Action": ["ecr:GetAuthorizationToken","ecr:BatchCheckLayerAvailability","ecr:GetDownloadUrlForLayer","ecr:BatchGetImage","ecr:InitiateLayerUpload","ecr:UploadLayerPart","ecr:CompleteLayerUpload","ecr:PutImage"],
      "Resource": "*" },
    { "Sid": "ECSDeploy", "Effect": "Allow",
      "Action": ["ecs:UpdateService","ecs:DescribeServices","ecs:DescribeTaskDefinition","ecs:RegisterTaskDefinition"],
      "Resource": "*" },
    { "Sid": "PassRole", "Effect": "Allow", "Action": "iam:PassRole",
      "Resource": "arn:aws:iam::${ACCT}:role/ecsTaskExecutionRole" }
  ]
}
"@
$polPassRole = @"
{
  "Version": "2012-10-17",
  "Statement": [{
    "Sid": "AllowPassEcsRoles", "Effect": "Allow", "Action": "iam:PassRole",
    "Resource": ["arn:aws:iam::${ACCT}:role/ecs-task-role","arn:aws:iam::${ACCT}:role/ecsTaskExecutionRole"],
    "Condition": { "StringEquals": { "iam:PassedToService": "ecs-tasks.amazonaws.com" } }
  }]
}
"@
$fTrust = Join-Path $PSScriptRoot "_trust.json"
$fEcr   = Join-Path $PSScriptRoot "_pol-ecr-deploy.json"
$fPass  = Join-Path $PSScriptRoot "_pol-passrole.json"
$trust        | Out-File $fTrust -Encoding ascii
$polEcrDeploy | Out-File $fEcr   -Encoding ascii
$polPassRole  | Out-File $fPass  -Encoding ascii

# ── 3. Rol ────────────────────────────────────────────────────────────────
$roleExists = $true
try { aws iam get-role --role-name $ROLE_NAME --profile $PROFILE_AWS 2>$null | Out-Null; if ($LASTEXITCODE -ne 0) { $roleExists = $false } } catch { $roleExists = $false }
if ($roleExists) {
    Write-Host "[=] Rol $ROLE_NAME ya existe; actualizo trust e inline policies."
    aws iam update-assume-role-policy --role-name $ROLE_NAME --policy-document "file://$fTrust" --profile $PROFILE_AWS | Out-Null
} else {
    aws iam create-role --role-name $ROLE_NAME --assume-role-policy-document "file://$fTrust" `
        --description "GitHub Actions OIDC deploy role" --profile $PROFILE_AWS | Out-Null
    Write-Host "[+] Rol creado."
}
aws iam put-role-policy --role-name $ROLE_NAME --policy-name "ECS-ECR-Deploy" --policy-document "file://$fEcr"  --profile $PROFILE_AWS | Out-Null
aws iam put-role-policy --role-name $ROLE_NAME --policy-name "ECS-PassRole"   --policy-document "file://$fPass" --profile $PROFILE_AWS | Out-Null
Write-Host "[+] Politicas inline aplicadas."

Remove-Item $fTrust,$fEcr,$fPass -Force

$ROLE_ARN = aws iam get-role --role-name $ROLE_NAME --profile $PROFILE_AWS --query "Role.Arn" --output text
Write-Host ""
Write-Host "ROLE_ARN = $ROLE_ARN"
Write-Host "Ahora setea el secret:  gh secret set AWS_ROLE_ARN  (con ese ARN)"
