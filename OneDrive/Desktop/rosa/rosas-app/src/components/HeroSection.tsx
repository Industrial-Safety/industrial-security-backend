"use client";
import Image from "next/image";

export default function HeroSection() {
  return (
    <section
      id="inicio"
      style={{
        position: "relative",
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        overflow: "hidden",
        background: "linear-gradient(135deg, #831843 0%, #be185d 30%, #a855f7 70%, #7c3aed 100%)",
      }}
    >
      {/* Decorative circles */}
      <div style={{ position: "absolute", top: -80, right: -80, width: 300, height: 300, borderRadius: "50%", background: "rgba(255,255,255,0.05)" }} />
      <div style={{ position: "absolute", bottom: -60, left: -60, width: 200, height: 200, borderRadius: "50%", background: "rgba(255,255,255,0.05)" }} />
      <div style={{ position: "absolute", top: "30%", left: "10%", fontSize: "3rem", opacity: 0.15 }} className="animate-float">🌸</div>
      <div style={{ position: "absolute", top: "20%", right: "15%", fontSize: "2rem", opacity: 0.15 }} className="animate-float delay-200">🌷</div>
      <div style={{ position: "absolute", bottom: "25%", left: "20%", fontSize: "2.5rem", opacity: 0.12 }} className="animate-float delay-400">💐</div>

      <div style={{ position: "relative", zIndex: 2, maxWidth: 1200, margin: "0 auto", padding: "6rem 1.5rem 4rem", display: "flex", flexWrap: "wrap", alignItems: "center", justifyContent: "center", gap: "3rem" }}>
        {/* Text */}
        <div style={{ flex: "1 1 400px", maxWidth: 550 }} className="animate-fade-in-up">
          <span style={{ display: "inline-block", background: "rgba(255,255,255,0.15)", backdropFilter: "blur(8px)", padding: "0.4rem 1rem", borderRadius: 50, color: "#fce7f3", fontSize: "0.85rem", fontWeight: 500, marginBottom: "1.2rem" }}>
            ✨ Día de la Madre 2026
          </span>
          <h1 style={{ fontFamily: "var(--font-heading)", fontSize: "clamp(2.2rem, 5vw, 3.5rem)", fontWeight: 700, color: "#fff", lineHeight: 1.15, marginBottom: "1rem" }}>
            Haz feliz a mamá con un detalle hecho con <span style={{ background: "linear-gradient(90deg, #fbbf24, #f472b6)", WebkitBackgroundClip: "text", WebkitTextFillColor: "transparent" }}>amor</span> 💐
          </h1>
          <p style={{ color: "rgba(255,255,255,0.85)", fontSize: "1.1rem", lineHeight: 1.7, marginBottom: "2rem", maxWidth: 480 }}>
            Arreglos florales artesanales hechos completamente con limpiapipas. Cada pétalo es moldeado a mano con dedicación y cariño 🌷
          </p>
          <div style={{ display: "flex", flexWrap: "wrap", gap: "1rem" }}>
            <a href="#catalogo" style={{ background: "linear-gradient(135deg, #fbbf24, #f59e0b)", color: "#1a1a2e", padding: "0.9rem 2rem", borderRadius: 50, fontWeight: 700, fontSize: "1rem", textDecoration: "none", transition: "transform 0.2s, box-shadow 0.2s", display: "inline-flex", alignItems: "center", gap: 8 }}
              onMouseEnter={(e) => { e.currentTarget.style.transform = "translateY(-2px)"; e.currentTarget.style.boxShadow = "0 8px 25px rgba(251,191,36,0.4)"; }}
              onMouseLeave={(e) => { e.currentTarget.style.transform = "translateY(0)"; e.currentTarget.style.boxShadow = "none"; }}
            >🌸 Ver Catálogo</a>
            <a href="https://wa.me/51935153080?text=Hola%20quiero%20hacer%20un%20pedido%20💐" target="_blank" rel="noopener noreferrer"
              style={{ background: "rgba(255,255,255,0.15)", backdropFilter: "blur(8px)", border: "1px solid rgba(255,255,255,0.3)", color: "#fff", padding: "0.9rem 2rem", borderRadius: 50, fontWeight: 600, fontSize: "1rem", textDecoration: "none", transition: "all 0.2s", display: "inline-flex", alignItems: "center", gap: 8 }}
              onMouseEnter={(e) => { e.currentTarget.style.background = "rgba(255,255,255,0.25)"; }}
              onMouseLeave={(e) => { e.currentTarget.style.background = "rgba(255,255,255,0.15)"; }}
            >💬 Comprar Ahora</a>
          </div>
        </div>

        {/* Hero image */}
        <div style={{ flex: "1 1 350px", maxWidth: 450, position: "relative" }} className="animate-fade-in-up delay-300">
          <div style={{ position: "relative", borderRadius: 24, overflow: "hidden", boxShadow: "0 25px 60px rgba(0,0,0,0.3)" }} className="animate-float">
            <Image src="/images/ramo-buchon-hero.png" alt="Ramo buchón artesanal de limpiapipas para el Día de la Madre" width={450} height={450} priority style={{ width: "100%", height: "auto", display: "block" }} />
          </div>
          <div style={{ position: "absolute", bottom: -10, right: -10, background: "rgba(255,255,255,0.95)", backdropFilter: "blur(8px)", borderRadius: 16, padding: "0.8rem 1.2rem", boxShadow: "0 8px 25px rgba(0,0,0,0.15)" }} className="animate-bounce-slow">
            <span style={{ fontSize: "0.75rem", color: "#64748b" }}>Hecho a mano</span>
            <div style={{ display: "flex", gap: 2, marginTop: 2 }}>
              {"⭐⭐⭐⭐⭐".split("").filter((_, i) => i % 2 === 0).map((s, i) => <span key={i}>⭐</span>)}
            </div>
          </div>
        </div>
      </div>

      {/* Scroll indicator */}
      <div style={{ position: "absolute", bottom: 30, left: "50%", transform: "translateX(-50%)", textAlign: "center" }} className="animate-bounce-slow">
        <span style={{ color: "rgba(255,255,255,0.6)", fontSize: "0.8rem" }}>Desliza hacia abajo</span>
        <div style={{ marginTop: 6, fontSize: "1.2rem", color: "rgba(255,255,255,0.5)" }}>↓</div>
      </div>
    </section>
  );
}
