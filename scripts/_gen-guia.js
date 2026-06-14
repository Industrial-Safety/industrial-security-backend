const fs = require("fs");
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  AlignmentType, LevelFormat, HeadingLevel, BorderStyle, WidthType, ShadingType,
  TableOfContents, PageNumber, Header, Footer, PageBreak
} = require("docx");

const CW = 9360; // content width US Letter, 1" margins

const border = { style: BorderStyle.SINGLE, size: 1, color: "C9C9C9" };
const borders = { top: border, bottom: border, left: border, right: border };
const cellMargins = { top: 80, bottom: 80, left: 120, right: 120 };

function h1(t){ return new Paragraph({ heading: HeadingLevel.HEADING_1, children:[new TextRun(t)] }); }
function h2(t){ return new Paragraph({ heading: HeadingLevel.HEADING_2, children:[new TextRun(t)] }); }
function p(t, opts={}){ return new Paragraph({ spacing:{after:120}, children:[new TextRun({text:t, ...opts})] }); }
function bullet(t){ return new Paragraph({ numbering:{reference:"b", level:0}, spacing:{after:60}, children:[new TextRun(t)] }); }
function num(t){ return new Paragraph({ numbering:{reference:"n", level:0}, spacing:{after:60}, children:[new TextRun(t)] }); }

// code block: monospace + light shading
function code(lines){
  const arr = Array.isArray(lines)? lines : [lines];
  return new Paragraph({
    spacing:{before:60, after:120},
    shading:{ fill:"F2F2F2", type:ShadingType.CLEAR },
    border:{ top:{style:BorderStyle.SINGLE,size:4,color:"D0D0D0",space:4}, bottom:{style:BorderStyle.SINGLE,size:4,color:"D0D0D0",space:4}, left:{style:BorderStyle.SINGLE,size:4,color:"D0D0D0",space:6}, right:{style:BorderStyle.SINGLE,size:4,color:"D0D0D0",space:6} },
    children: arr.flatMap((l,i)=> i===0? [new TextRun({text:l, font:"Consolas", size:19})] : [new TextRun({text:l, font:"Consolas", size:19, break:1})])
  });
}

function paramTable(rows){
  const head = new TableRow({ tableHeader:true, children:[
    th("Parámetro"), th("Para qué sirve"),
  ]});
  const body = rows.map(r=> new TableRow({ children:[ td(r[0], true), td(r[1]) ] }));
  return new Table({ width:{size:CW,type:WidthType.DXA}, columnWidths:[2600,6760], rows:[head,...body] });
}
function th(t){ return new TableCell({ borders, width:{size:0,type:WidthType.DXA}, shading:{fill:"2E5A88",type:ShadingType.CLEAR}, margins:cellMargins, children:[new Paragraph({children:[new TextRun({text:t,bold:true,color:"FFFFFF"})]})] }); }
function td(t, mono=false){ return new TableCell({ borders, margins:cellMargins, children:[new Paragraph({children:[new TextRun({text:t, font: mono?"Consolas":"Arial", size: mono?19:22})]})] }); }
// fix widths on header cells
function th2(t,w){ return new TableCell({ borders, width:{size:w,type:WidthType.DXA}, shading:{fill:"2E5A88",type:ShadingType.CLEAR}, margins:cellMargins, children:[new Paragraph({children:[new TextRun({text:t,bold:true,color:"FFFFFF"})]})] }); }
function tdw(t,w,mono=false){ return new TableCell({ borders, width:{size:w,type:WidthType.DXA}, margins:cellMargins, children:[new Paragraph({children:[new TextRun({text:t, font:mono?"Consolas":"Arial", size:mono?19:22})]})] }); }
function ptable(cols, rows){
  const head = new TableRow({ tableHeader:true, children: cols.map(c=>th2(c.t,c.w)) });
  const body = rows.map(r=> new TableRow({ children: r.map((cell,i)=> tdw(cell, cols[i].w, cols[i].mono)) }));
  return new Table({ width:{size:CW,type:WidthType.DXA}, columnWidths: cols.map(c=>c.w), rows:[head,...body] });
}

const children = [];

// ---- Title ----
children.push(new Paragraph({ spacing:{before:1200, after:120}, alignment:AlignmentType.CENTER, children:[new TextRun({text:"Guía de Scripts de Operación", bold:true, size:52, color:"2E5A88"})]}));
children.push(new Paragraph({ alignment:AlignmentType.CENTER, spacing:{after:80}, children:[new TextRun({text:"SafeIndustrial Backend — Infraestructura AWS", size:30, color:"555555"})]}));
children.push(new Paragraph({ alignment:AlignmentType.CENTER, spacing:{after:400}, children:[new TextRun({text:"Cómo ejecutar los scripts desde tu laptop", size:24, italics:true, color:"777777"})]}));
children.push(new Paragraph({ alignment:AlignmentType.CENTER, children:[new TextRun({text:"Cuenta AWS 160631388633 · Región us-east-1 · Cluster industrial-safety-cluster", size:20, color:"888888"})]}));
children.push(new Paragraph({ children:[new PageBreak()] }));

// ---- TOC ----
children.push(h1("Contenido"));
children.push(new TableOfContents("Contenido", { hyperlink:true, headingStyleRange:"1-2" }));
children.push(new Paragraph({ children:[new PageBreak()] }));

// ---- 1. Intro ----
children.push(h1("1. Introducción"));
children.push(p("Este documento explica los scripts de operación del backend SafeIndustrial y cómo ejecutarlos desde tu laptop con Windows. Los scripts automatizan tareas sobre AWS: encender/apagar el entorno, crear alarmas de monitoreo, configurar el autoscaling, mejorar la base de datos y corregir configuraciones."));
children.push(p("Todos los scripts están en la carpeta scripts\\ del proyecto y usan la AWS CLI por debajo. No modifican código: solo ejecutan comandos contra tu cuenta de AWS."));

// ---- 2. Requisitos ----
children.push(h1("2. Requisitos previos en tu laptop"));
children.push(p("Antes de ejecutar cualquier script, tu laptop debe tener lo siguiente:"));
children.push(bullet("AWS CLI instalada. Verifícala con: aws --version"));
children.push(bullet("AWS CLI configurada con tus credenciales y la región us-east-1."));
children.push(bullet("PowerShell (viene con Windows)."));
children.push(bullet("El repositorio del proyecto clonado/descargado en la laptop."));
children.push(bullet("(Opcional) k6, solo si vas a correr pruebas de carga."));
children.push(h2("2.1 Configurar la AWS CLI (una sola vez)"));
children.push(p("Si es una laptop nueva, configura tus credenciales:"));
children.push(code(["aws configure","# AWS Access Key ID:     <tu access key>","# AWS Secret Access Key: <tu secret>","# Default region name:   us-east-1","# Default output format:  json"]));
children.push(p("Verifica que quedó bien conectada (debe mostrar la cuenta 160631388633):"));
children.push(code("aws sts get-caller-identity"));

// ---- 3. Como ejecutar ----
children.push(h1("3. Cómo ejecutar un script (general)"));
children.push(num("Abre PowerShell."));
children.push(num("Ve a la carpeta del proyecto. Ejemplo:"));
children.push(code('cd "C:\\Users\\panc1\\OneDrive\\Documentos\\backend-integrador\\industrial-security-backend"'));
children.push(num("Ejecuta el script con .\\scripts\\ adelante y sus parámetros. Ejemplo:"));
children.push(code(".\\scripts\\levantar-entorno.ps1 -Status"));
children.push(p("Si PowerShell bloquea la ejecución por la política de seguridad, córrelo así (solo para esa ventana):", {bold:true}));
children.push(code("powershell -ExecutionPolicy Bypass -File .\\scripts\\levantar-entorno.ps1 -Status"));

// ---- 4. Scripts ----
children.push(new Paragraph({ children:[new PageBreak()] }));
children.push(h1("4. Los scripts"));

// 4.1 levantar-entorno
children.push(h2("4.1 levantar-entorno.ps1 — Encender / Apagar / Estado"));
children.push(p("El script más importante del día a día. Enciende o apaga TODO el entorno (base de datos RDS + los 13 servicios ECS) con un solo comando. Úsalo para no pagar cuando no estás trabajando."));
children.push(p("Parámetros (elige una acción):", {bold:true}));
children.push(ptable([{t:"Parámetro",w:2200},{t:"Qué hace",w:7160}],[
  ["-Up","Arranca la RDS, espera a que esté lista y pone los servicios en 1 tarea."],
  ["-Down","Apaga todo: quita el autoscaling, pone los servicios en 0 y detiene la RDS."],
  ["-Status","Muestra el estado actual (RDS y cada servicio)."],
]));
children.push(p("Ejemplos:", {bold:true}));
children.push(code([".\\scripts\\levantar-entorno.ps1 -Up        # encender todo",".\\scripts\\levantar-entorno.ps1 -Status    # ver estado",".\\scripts\\levantar-entorno.ps1 -Down      # apagar todo (deja de pagar)"]));
children.push(p("Nota: la RDS tarda 5-10 min en arrancar; el entorno queda 100% listo unos minutos después de -Up. keycloak y ngrok no están incluidos en este script (ver sección 5).", {italics:true}));

// 4.2 prender-entorno
children.push(h2("4.2 prender-entorno.ps1 — Encendido automático"));
children.push(p("Versión de \"solo encender\" pensada para la Tarea Programada de Windows (por ejemplo, encender el entorno a las 2pm). Arranca la RDS, espera y enciende los 13 servicios. No recibe parámetros."));
children.push(code(".\\scripts\\prender-entorno.ps1"));
children.push(p("Deja un registro en scripts\\prender.log. Recuerda: para que la tarea programada funcione, la laptop debe estar encendida a esa hora.", {italics:true}));

// 4.3 alarmas
children.push(h2("4.3 setup-alarmas-consumo.ps1 — Alarmas de CPU, RAM y Disco"));
children.push(p("Crea las alarmas de CloudWatch que vigilan el consumo (CPU y RAM de los servicios; CPU, RAM y disco de la base de datos) y te avisan por correo. También crea el tema SNS y suscribe tu correo."));
children.push(ptable([{t:"Parámetro",w:2200},{t:"Qué hace",w:7160}],[
  ["-Email","(Obligatorio) Correo donde recibir las alarmas."],
  ["-Test","Fuerza una alarma a estado ALARM para probar que te llega el correo."],
]));
children.push(p("Ejemplos:", {bold:true}));
children.push(code([".\\scripts\\setup-alarmas-consumo.ps1 -Email \"ricardoismael777@gmail.com\"",".\\scripts\\setup-alarmas-consumo.ps1 -Email \"ricardoismael777@gmail.com\" -Test"]));
children.push(p("IMPORTANTE: tras crear la suscripción, abre tu correo y haz clic en \"Confirm subscription\". Hasta que confirmes, AWS no envía ninguna alarma.", {bold:true}));

// 4.4 apply-capacity-plan
children.push(h2("4.4 apply-capacity-plan.ps1 — Plan de Capacidad"));
children.push(p("Aplica las acciones del Plan de Capacidad y Rendimiento. Cada sección es independiente: ejecuta solo la que quieras con su switch."));
children.push(ptable([{t:"Parámetro",w:2400},{t:"Qué hace / Costo",w:6960}],[
  ["-CircuitBreaker","Rollback automático de despliegues ECS. Gratis."],
  ["-Alarms","SNS + alarmas (CPU, conexiones RDS, backlog de colas, DLQs). ~US$5-10/mes."],
  ["-Rds","Sube la RDS a t4g.medium Multi-AZ + gp3. ~+US$70-85/mes."],
  ["-ReadReplica","Crea una réplica de lectura (BD copia para descargar lecturas). ~+US$50/mes."],
  ["-AutoScaling","Registra el autoscaling (min/max tareas, CPU objetivo). Enciende tareas (factura)."],
]));
children.push(p("Ejemplos:", {bold:true}));
children.push(code([".\\scripts\\apply-capacity-plan.ps1 -Alarms -CircuitBreaker",".\\scripts\\apply-capacity-plan.ps1 -AutoScaling",".\\scripts\\apply-capacity-plan.ps1 -Rds"]));
children.push(p("Orden recomendado: CircuitBreaker y Alarms cuando quieras (no facturan tareas); Rds ANTES de AutoScaling.", {italics:true}));

// 4.5 fix solicitudes
children.push(h2("4.5 fix-solicitudes-rabbitmq.ps1 — Corregir solicitudes"));
children.push(p("Corrige el host de RabbitMQ de solicitudes-service (si quedó apuntando a un broker viejo) y, opcionalmente, configura el token de JIRA. Registra una task definition nueva y redespliega."));
children.push(ptable([{t:"Parámetro",w:2400},{t:"Qué hace",w:6960}],[
  ["-JiraToken","(Opcional) Token de Atlassian para habilitar la integración con JIRA."],
]));
children.push(code([".\\scripts\\fix-solicitudes-rabbitmq.ps1",".\\scripts\\fix-solicitudes-rabbitmq.ps1 -JiraToken \"ATATT...tu-token\""]));

// 4.6 k6
children.push(h2("4.6 Pruebas de carga (carpeta load\\)"));
children.push(p("Scripts de k6 para simular carga y ver el autoscaling en acción. Requieren tener k6 instalado (winget install k6). Generan tráfico real contra la nube."));
children.push(p("Importante: por ngrok gratuito la carga se throttlea; para una prueba real conviene apuntar a la IP pública del gateway en el puerto 9000.", {italics:true}));
children.push(code([".\\;k6 run -e GATEWAY_URL=https://<tu-url> -e KEYCLOAK_URL=http://industrial-safety.duckdns.org:8080 `","  -e CLIENT_ID=industrial-safety-client -e CLIENT_SECRET=<secret> `","  -e TEST_USER=<usuario> -e TEST_PASS=<password> load\\lt02-demo.js"]));

// ---- 5. Cheat sheet ----
children.push(new Paragraph({ children:[new PageBreak()] }));
children.push(h1("5. Operaciones comunes (resumen rápido)"));
children.push(ptable([{t:"Quiero...",w:3600},{t:"Comando",w:5760, mono:true}],[
  ["Ver el estado del entorno",".\\scripts\\levantar-entorno.ps1 -Status"],
  ["Encender todo",".\\scripts\\levantar-entorno.ps1 -Up"],
  ["Apagar todo (no pagar)",".\\scripts\\levantar-entorno.ps1 -Down"],
  ["Encender keycloak (aparte)","aws ecs update-service --cluster industrial-safety-cluster --service industrial-safety-keycloak-task-service-l6iivm6a --desired-count 1 --region us-east-1"],
  ["Encender ngrok (aparte)","aws ecs update-service --cluster industrial-safety-cluster --service ngrok-tunnel-service --desired-count 1 --region us-east-1"],
  ["Crear alarmas + correo",".\\scripts\\setup-alarmas-consumo.ps1 -Email \"tucorreo@gmail.com\""],
  ["Ver alarmas","aws cloudwatch describe-alarms --query \"MetricAlarms[].{N:AlarmName,E:StateValue}\" --output table --region us-east-1"],
]));

// ---- 6. Notas ----
children.push(h1("6. Notas y advertencias importantes"));
children.push(bullet("Costo fijo: Amazon MQ (RabbitMQ) no se puede apagar y factura ~US$60/mes aunque todo lo demás esté apagado. ECR cobra centavos por almacenamiento."));
children.push(bullet("keycloak con IP dinámica: cada vez que keycloak reinicia, su IP pública cambia y hay que actualizar el registro DuckDNS, o el login se rompe. Comando: abrir https://www.duckdns.org/update?domains=industrial-safety&token=TU_TOKEN&ip=NUEVA_IP"));
children.push(bullet("ngrok gratuito: limita el tráfico (throttling). Para pruebas de carga reales, pegar a la IP pública del gateway en :9000."));
children.push(bullet("Apagar siempre al terminar: corre -Down para no acumular costos de las tareas Fargate. keycloak se apaga aparte (desired-count 0)."));
children.push(bullet("Los scripts no cambian código: solo ejecutan acciones en AWS. Son reversibles (encender/apagar, crear/borrar alarmas)."));

const doc = new Document({
  styles: {
    default:{ document:{ run:{ font:"Arial", size:22 } } },
    paragraphStyles:[
      { id:"Heading1", name:"Heading 1", basedOn:"Normal", next:"Normal", quickFormat:true, run:{size:30,bold:true,color:"2E5A88",font:"Arial"}, paragraph:{spacing:{before:280,after:160}, outlineLevel:0} },
      { id:"Heading2", name:"Heading 2", basedOn:"Normal", next:"Normal", quickFormat:true, run:{size:25,bold:true,color:"3A6CA0",font:"Arial"}, paragraph:{spacing:{before:200,after:120}, outlineLevel:1} },
    ]
  },
  numbering:{ config:[
    { reference:"b", levels:[{level:0,format:LevelFormat.BULLET,text:"•",alignment:AlignmentType.LEFT,style:{paragraph:{indent:{left:720,hanging:360}}}}] },
    { reference:"n", levels:[{level:0,format:LevelFormat.DECIMAL,text:"%1.",alignment:AlignmentType.LEFT,style:{paragraph:{indent:{left:720,hanging:360}}}}] },
  ]},
  sections:[{
    properties:{ page:{ size:{width:12240,height:15840}, margin:{top:1440,right:1440,bottom:1440,left:1440} } },
    footers:{ default:new Footer({ children:[new Paragraph({ alignment:AlignmentType.CENTER, children:[new TextRun({text:"SafeIndustrial — Guía de Scripts · página ", size:18, color:"888888"}), new TextRun({children:[PageNumber.CURRENT], size:18, color:"888888"})] })] }) },
    children
  }]
});

Packer.toBuffer(doc).then(b=>{ fs.writeFileSync("Guia_Scripts_SafeIndustrial.docx", b); console.log("OK Guia_Scripts_SafeIndustrial.docx"); });
