"use client";
import { useState, useEffect } from "react";

export default function Navbar() {
  const [scrolled, setScrolled] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 40);
    window.addEventListener("scroll", onScroll);
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  const navLinks = [
    { href: "#inicio", label: "Inicio" },
    { href: "#catalogo", label: "Catálogo" },
    { href: "#nosotros", label: "Nosotros" },
    { href: "#como-pedir", label: "Cómo Pedir" },
    { href: "#contacto", label: "Contacto" },
  ];

  return (
    <nav
      id="navbar"
      style={{
        position: "fixed",
        top: 0,
        left: 0,
        right: 0,
        zIndex: 1000,
        transition: "all 0.3s ease",
        background: scrolled ? "rgba(255,251,254,0.95)" : "transparent",
        backdropFilter: scrolled ? "blur(12px)" : "none",
        boxShadow: scrolled ? "0 2px 20px rgba(236,72,153,0.1)" : "none",
        padding: scrolled ? "0.6rem 0" : "1rem 0",
      }}
    >
      <div style={{ maxWidth: 1200, margin: "0 auto", padding: "0 1.5rem", display: "flex", alignItems: "center", justifyContent: "space-between" }}>
        <a href="#inicio" style={{ textDecoration: "none", display: "flex", alignItems: "center", gap: "0.5rem" }}>
          <span style={{ fontSize: "1.8rem" }}>🌷</span>
          <span style={{ fontFamily: "var(--font-heading)", fontSize: "1.3rem", fontWeight: 700, background: "linear-gradient(135deg, #ec4899, #a855f7)", WebkitBackgroundClip: "text", WebkitTextFillColor: "transparent" }}>
            YYDetalles
          </span>
          <span style={{ fontSize: "0.75rem", color: "#ec4899", fontWeight: 500 }}>con Amor</span>
        </a>

        {/* Desktop links */}
        <div style={{ display: "flex", gap: "2rem", alignItems: "center" }} className="hidden md:flex">
          {navLinks.map((l) => (
            <a key={l.href} href={l.href} style={{ textDecoration: "none", color: scrolled ? "#1a1a2e" : "#fff", fontSize: "0.9rem", fontWeight: 500, transition: "color 0.3s" }}
              onMouseEnter={(e) => (e.currentTarget.style.color = "#ec4899")}
              onMouseLeave={(e) => (e.currentTarget.style.color = scrolled ? "#1a1a2e" : "#fff")}
            >{l.label}</a>
          ))}
          <a href="https://wa.me/51935153080?text=Hola%20quiero%20hacer%20un%20pedido%20💐" target="_blank" rel="noopener noreferrer"
            style={{ background: "linear-gradient(135deg, #ec4899, #a855f7)", color: "#fff", padding: "0.5rem 1.2rem", borderRadius: 50, fontSize: "0.85rem", fontWeight: 600, textDecoration: "none", transition: "transform 0.2s, box-shadow 0.2s" }}
            onMouseEnter={(e) => { e.currentTarget.style.transform = "scale(1.05)"; e.currentTarget.style.boxShadow = "0 4px 15px rgba(236,72,153,0.4)"; }}
            onMouseLeave={(e) => { e.currentTarget.style.transform = "scale(1)"; e.currentTarget.style.boxShadow = "none"; }}
          >💬 WhatsApp</a>
        </div>

        {/* Mobile hamburger */}
        <button className="md:hidden" onClick={() => setMenuOpen(!menuOpen)}
          style={{ background: "none", border: "none", cursor: "pointer", padding: 8 }}
          aria-label="Abrir menú"
        >
          <div style={{ width: 24, height: 2, background: scrolled ? "#1a1a2e" : "#fff", transition: "all 0.3s", transform: menuOpen ? "rotate(45deg) translate(5px,5px)" : "none" }} />
          <div style={{ width: 24, height: 2, background: scrolled ? "#1a1a2e" : "#fff", transition: "all 0.3s", margin: "5px 0", opacity: menuOpen ? 0 : 1 }} />
          <div style={{ width: 24, height: 2, background: scrolled ? "#1a1a2e" : "#fff", transition: "all 0.3s", transform: menuOpen ? "rotate(-45deg) translate(5px,-5px)" : "none" }} />
        </button>
      </div>

      {/* Mobile menu */}
      {menuOpen && (
        <div className="md:hidden animate-slide-down" style={{ background: "rgba(255,251,254,0.98)", backdropFilter: "blur(12px)", padding: "1rem 1.5rem", borderTop: "1px solid rgba(236,72,153,0.1)" }}>
          {navLinks.map((l) => (
            <a key={l.href} href={l.href} onClick={() => setMenuOpen(false)}
              style={{ display: "block", padding: "0.75rem 0", color: "#1a1a2e", textDecoration: "none", fontWeight: 500, borderBottom: "1px solid rgba(236,72,153,0.08)" }}
            >{l.label}</a>
          ))}
          <a href="https://wa.me/51935153080?text=Hola%20quiero%20hacer%20un%20pedido%20💐" target="_blank" rel="noopener noreferrer"
            style={{ display: "block", marginTop: "0.75rem", textAlign: "center", background: "linear-gradient(135deg, #ec4899, #a855f7)", color: "#fff", padding: "0.7rem", borderRadius: 50, textDecoration: "none", fontWeight: 600 }}
          >💬 Pedir por WhatsApp</a>
        </div>
      )}
    </nav>
  );
}
