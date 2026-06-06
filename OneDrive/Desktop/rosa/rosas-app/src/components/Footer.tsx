"use client";
export default function Footer() {
  return (
    <footer id="contacto" style={{ background: "linear-gradient(135deg, #1a1a2e, #2d1b3d)", color: "#fff", padding: "4rem 1.5rem 2rem" }}>
      <div style={{ maxWidth: 1200, margin: "0 auto" }}>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: "2.5rem", marginBottom: "3rem" }}>
          {/* Brand */}
          <div>
            <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: "1rem" }}>
              <span style={{ fontSize: "2rem" }}>🌷</span>
              <div>
                <div style={{ fontFamily: "var(--font-heading)", fontSize: "1.3rem", fontWeight: 700 }}>YYDetalles</div>
                <div style={{ fontSize: "0.75rem", color: "#f9a8d4" }}>con Amor</div>
              </div>
            </div>
            <p style={{ color: "rgba(255,255,255,0.6)", fontSize: "0.85rem", lineHeight: 1.6 }}>
              Arreglos florales artesanales hechos con limpiapipas. Cada pétalo lleva amor y dedicación ✨
            </p>
          </div>

          {/* Links */}
          <div>
            <h4 style={{ fontFamily: "var(--font-heading)", fontSize: "1rem", fontWeight: 600, marginBottom: "1rem", color: "#f9a8d4" }}>Navegación</h4>
            <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
              {["Inicio", "Catálogo", "Nosotros", "Cómo Pedir"].map((l) => (
                <a key={l} href={`#${l.toLowerCase().replace(/\s/g, "-").replace("ó", "o")}`}
                  style={{ color: "rgba(255,255,255,0.6)", textDecoration: "none", fontSize: "0.85rem", transition: "color 0.3s" }}
                  onMouseEnter={(e) => (e.currentTarget.style.color = "#f9a8d4")}
                  onMouseLeave={(e) => (e.currentTarget.style.color = "rgba(255,255,255,0.6)")}
                >{l}</a>
              ))}
            </div>
          </div>

          {/* Contact */}
          <div>
            <h4 style={{ fontFamily: "var(--font-heading)", fontSize: "1rem", fontWeight: 600, marginBottom: "1rem", color: "#f9a8d4" }}>Contacto</h4>
            <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
              <a href="https://wa.me/51935153080" target="_blank" rel="noopener noreferrer" style={{ color: "rgba(255,255,255,0.6)", textDecoration: "none", fontSize: "0.85rem", transition: "color 0.3s" }}
                onMouseEnter={(e) => (e.currentTarget.style.color = "#25d366")}
                onMouseLeave={(e) => (e.currentTarget.style.color = "rgba(255,255,255,0.6)")}
              >📱 935 153 080</a>
              <a href="https://wa.me/51908554594" target="_blank" rel="noopener noreferrer" style={{ color: "rgba(255,255,255,0.6)", textDecoration: "none", fontSize: "0.85rem", transition: "color 0.3s" }}
                onMouseEnter={(e) => (e.currentTarget.style.color = "#25d366")}
                onMouseLeave={(e) => (e.currentTarget.style.color = "rgba(255,255,255,0.6)")}
              >📱 908 554 594</a>
              <a href="https://instagram.com/yydetallesconamor10" target="_blank" rel="noopener noreferrer" style={{ color: "rgba(255,255,255,0.6)", textDecoration: "none", fontSize: "0.85rem", transition: "color 0.3s" }}
                onMouseEnter={(e) => (e.currentTarget.style.color = "#e1306c")}
                onMouseLeave={(e) => (e.currentTarget.style.color = "rgba(255,255,255,0.6)")}
              >📸 @yydetallesconamor10</a>
            </div>
          </div>

          {/* Delivery */}
          <div>
            <h4 style={{ fontFamily: "var(--font-heading)", fontSize: "1rem", fontWeight: 600, marginBottom: "1rem", color: "#f9a8d4" }}>Entregas</h4>
            <p style={{ color: "rgba(255,255,255,0.6)", fontSize: "0.85rem", lineHeight: 1.6 }}>
              📍 Arequipa: El Avelino, Mall Porongoche, Cementerio Apacheta<br/>
              🚚 Envíos nacionales por Shalom<br/>
              ⏰ Pedidos con 3-5 días de anticipación
            </p>
          </div>
        </div>

        <div style={{ borderTop: "1px solid rgba(255,255,255,0.08)", paddingTop: "1.5rem", textAlign: "center" }}>
          <p style={{ color: "rgba(255,255,255,0.4)", fontSize: "0.8rem" }}>
            © 2026 YYDetalles con Amor — Hecho con 💕 en Perú
          </p>
        </div>
      </div>
    </footer>
  );
}
