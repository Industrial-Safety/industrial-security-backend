package com.logistica.purchase.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "inventory")
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codigo;
    private String descripcion;
    private String lote;
    private String vencimiento;
    private Integer stock;
    private String estado;

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getLote() { return lote; }
    public void setLote(String lote) { this.lote = lote; }

    public String getVencimiento() { return vencimiento; }
    public void setVencimiento(String vencimiento) { this.vencimiento = vencimiento; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
