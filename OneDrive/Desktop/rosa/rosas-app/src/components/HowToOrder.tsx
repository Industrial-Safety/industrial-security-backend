"use client";
export default function HowToOrder() {
  const steps = [
    { num: "01", icon: "🌸", title: "Escoge tu arreglo", desc: "Explora nuestro catálogo y elige el diseño que más te guste." },
    { num: "02", icon: "💬", title: "Escríbenos", desc: "Contáctanos por WhatsApp al 935 153 080 o 908 554 594." },
    { num: "03", icon: "📋", title: "Envía tus datos", desc: "Nombre, dirección y fecha de entrega. ¡También acepta imágenes de referencia!" },
    { num: "04", icon: "🎁", title: "¡Recibe tu regalo!", desc: "Preparamos tu pedido con amor y lo enviamos a donde lo necesites." },
  ];
  return (
    <section id="como-pedir" style={{ padding: "5rem 1.5rem", maxWidth: 1200, margin: "0 auto" }}>
      <div style={{ textAlign: "center", marginBottom: "3rem" }}>
        <span style={{ display: "inline-block", background: "linear-gradient(135deg, #fce7f3, #f3e8ff)", padding: "0.4rem 1.2rem", borderRadius: 50, fontSize: "0.85rem", fontWeight: 500, color: "#be185d", marginBottom: "0.8rem" }}>
          🛒 Proceso de Compra
        </span>
        <h2 style={{ fontFamily: "var(--font-heading)", fontSize: "clamp(1.8rem, 4vw, 2.5rem)", fontWeight: 700, color: "#1a1a2e", marginBottom: "0.5rem" }}>
          ¿Cómo hacer tu pedido?
        </h2>
        <p style={{ color: "#64748b", maxWidth: 500, margin: "0 auto", lineHeight: 1.6 }}>
          Es muy fácil. Solo sigue estos 4 pasos y haz feliz a mamá 💐
        </p>
      </div>
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: "1.5rem" }}>
        {steps.map((s, i) => (
          <div
            key={i}
            className="animate-fade-in-up"
            style={{
              animationDelay: `${i * 0.15}s`,
              position: "relative",
              background: "linear-gradient(135deg, #fdf2f8, #faf5ff)",
              borderRadius: 20,
              padding: "2rem 1.5rem",
              textAlign: "center",
              border: "1px solid rgba(236,72,153,0.1)",
              transition: "transform 0.3s",
            }}
            onMouseEnter={(e) => (e.currentTarget.style.transform = "translateY(-4px)")}
            onMouseLeave={(e) => (e.currentTarget.style.transform = "translateY(0)")}
          >
            <div style={{ position: "absolute", top: 12, right: 16, fontFamily: "var(--font-heading)", fontSize: "2.5rem", fontWeight: 700, color: "rgba(236,72,153,0.08)" }}>{s.num}</div>
            <div style={{ fontSize: "2.5rem", marginBottom: "1rem" }}>{s.icon}</div>
            <h3 style={{ fontFamily: "var(--font-heading)", fontSize: "1.1rem", fontWeight: 600, color: "#1a1a2e", marginBottom: 8 }}>{s.title}</h3>
            <p style={{ color: "#64748b", fontSize: "0.85rem", lineHeight: 1.5 }}>{s.desc}</p>
          </div>
        ))}
      </div>
      {/* Delivery info */}
      <div style={{ marginTop: "3rem", background: "linear-gradient(135deg, #831843, #a855f7)", borderRadius: 24, padding: "2.5rem", color: "#fff", display: "flex", flexWrap: "wrap", gap: "2rem", alignItems: "center", justifyContent: "space-between" }}>
        <div style={{ flex: "1 1 300px" }}>
          <h3 style={{ fontFamily: "var(--font-heading)", fontSize: "1.5rem", fontWeight: 700, marginBottom: 10 }}>📦 Envíos y Entregas</h3>
          <ul style={{ listStyle: "none", display: "flex", flexDirection: "column", gap: 8 }}>
            <li style={{ fontSize: "0.9rem", opacity: 0.9 }}>⏰ Pedidos con 3 a 5 días de anticipación</li>
            <li style={{ fontSize: "0.9rem", opacity: 0.9 }}>🚚 Envíos a todo el Perú (Shalom)</li>
            <li style={{ fontSize: "0.9rem", opacity: 0.9 }}>📍 Arequipa: El Avelino, Mall Porongoche, Cementerio Apacheta</li>
            <li style={{ fontSize: "0.9rem", opacity: 0.9 }}>💸 Costo de envío varía según distancia</li>
          </ul>
        </div>
        <div style={{ flex: "0 0 auto" }}>
          <a href="https://wa.me/51935153080?text=Hola%20quiero%20consultar%20sobre%20envíos%20📦" target="_blank" rel="noopener noreferrer"
            style={{ display: "inline-flex", alignItems: "center", gap: 8, background: "#fff", color: "#831843", padding: "0.9rem 2rem", borderRadius: 50, fontWeight: 700, textDecoration: "none", transition: "transform 0.2s, box-shadow 0.2s" }}
            onMouseEnter={(e) => { e.currentTarget.style.transform = "scale(1.05)"; e.currentTarget.style.boxShadow = "0 8px 25px rgba(0,0,0,0.2)"; }}
            onMouseLeave={(e) => { e.currentTarget.style.transform = "scale(1)"; e.currentTarget.style.boxShadow = "none"; }}
          >💬 Consultar Envío</a>
        </div>
      </div>
    </section>
  );
}
