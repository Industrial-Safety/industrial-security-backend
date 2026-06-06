"use client";
export default function AboutSection() {
  const features = [
    { icon: "🌸", title: "100% Artesanal", desc: "Cada flor es moldeada a mano con limpiapipas de colores." },
    { icon: "💎", title: "Durabilidad", desc: "A diferencia de flores naturales, las nuestras duran para siempre." },
    { icon: "🎨", title: "Personalizable", desc: "Elige colores, agrega peluches, mariposas y chocolates." },
    { icon: "🚚", title: "Envío Nacional", desc: "Envíos a todo el Perú mediante Shalom." },
  ];
  return (
    <section id="nosotros" style={{ padding: "5rem 1.5rem", background: "linear-gradient(180deg, #fdf2f8, #faf5ff, #fff)" }}>
      <div style={{ maxWidth: 1200, margin: "0 auto" }}>
        <div style={{ textAlign: "center", marginBottom: "3rem" }}>
          <span style={{ display: "inline-block", background: "linear-gradient(135deg, #fce7f3, #f3e8ff)", padding: "0.4rem 1.2rem", borderRadius: 50, fontSize: "0.85rem", fontWeight: 500, color: "#be185d", marginBottom: "0.8rem" }}>
            🌷 Sobre Nosotros
          </span>
          <h2 style={{ fontFamily: "var(--font-heading)", fontSize: "clamp(1.8rem, 4vw, 2.5rem)", fontWeight: 700, color: "#1a1a2e", marginBottom: "0.5rem" }}>
            Porque cada mamá merece sentirse especial
          </h2>
          <p style={{ color: "#64748b", maxWidth: 550, margin: "0 auto", lineHeight: 1.6 }}>
            Somos un emprendimiento peruano que crea arreglos florales únicos con limpiapipas. Cada pieza lleva horas de dedicación artesanal ✨
          </p>
        </div>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))", gap: "1.5rem" }}>
          {features.map((f, i) => (
            <div
              key={i}
              className="animate-fade-in-up"
              style={{
                animationDelay: `${i * 0.15}s`,
                background: "#fff",
                borderRadius: 20,
                padding: "2rem 1.5rem",
                textAlign: "center",
                boxShadow: "0 4px 15px rgba(0,0,0,0.04)",
                border: "1px solid rgba(236,72,153,0.08)",
                transition: "transform 0.3s, box-shadow 0.3s",
                cursor: "default",
              }}
              onMouseEnter={(e) => { e.currentTarget.style.transform = "translateY(-6px)"; e.currentTarget.style.boxShadow = "0 15px 35px rgba(236,72,153,0.12)"; }}
              onMouseLeave={(e) => { e.currentTarget.style.transform = "translateY(0)"; e.currentTarget.style.boxShadow = "0 4px 15px rgba(0,0,0,0.04)"; }}
            >
              <div style={{ fontSize: "2.5rem", marginBottom: "1rem" }}>{f.icon}</div>
              <h3 style={{ fontFamily: "var(--font-heading)", fontSize: "1.15rem", fontWeight: 600, color: "#1a1a2e", marginBottom: 8 }}>{f.title}</h3>
              <p style={{ color: "#64748b", fontSize: "0.88rem", lineHeight: 1.5 }}>{f.desc}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
