"use client";
import { useState } from "react";
import Image from "next/image";

type Product = {
  id: number;
  name: string;
  category: string;
  image: string;
  description: string;
  rating: number;
  tags: string[];
};

const products: Product[] = [
  { id: 1, name: "Ramo Buchón Imperial", category: "buchones", image: "/images/ramo-buchon-grande.png", description: "Ramo buchón abundante con rosas de limpiapipas en tonos rojos, rosados y blancos. Envuelto en papel elegante.", rating: 5.0, tags: ["Popular", "Buchón"] },
  { id: 2, name: "Dulce Primavera", category: "ramos", image: "/images/ramo-rosas-rosadas.png", description: "Hermoso ramo de rosas rosadas artesanales, ideal para demostrar amor y cariño.", rating: 4.8, tags: ["Rosas"] },
  { id: 3, name: "Jardín de Colores", category: "ramos", image: "/images/Gemini_Generated_Image_j51l2vj51l2vj51l.png", description: "Variedad de flores de colores hechas a mano con limpiapipas: tulipanes, margaritas y rosas.", rating: 4.9, tags: ["Colorido"] },
  { id: 4, name: "Caja de Amor", category: "cajas", image: "/images/arreglo-caja-flores.png", description: "Elegante caja decorativa con rosas pastel de limpiapipas, mariposas y detalles especiales.", rating: 5.0, tags: ["Premium", "Caja"] },
  { id: 5, name: "Sol de Mamá", category: "ramos", image: "/images/ramo-girasoles.png", description: "Ramo de girasoles y flores silvestres artesanales, alegría pura para mamá.", rating: 4.7, tags: ["Girasoles"] },
  { id: 6, name: "Lirio Dorado", category: "ramos", image: "/images/imagen de petalo amarrilo.png", description: "Delicado arreglo con lirios amarillos y lavanda de limpiapipas. Elegancia natural.", rating: 4.6, tags: ["Elegante"] },
  { id: 7, name: "Osito Cariñoso", category: "adicionales", image: "/images/imagen de oso.png", description: "Peluche osito con chocolates y mariposa dorada. Perfecto para complementar tu ramo.", rating: 4.9, tags: ["Peluche", "Chocolates"] },
  { id: 8, name: "Arte Floral", category: "ramos", image: "/images/imagen de armado.png", description: "Mira cómo elaboramos cada flor pétalo a pétalo con dedicación artesanal.", rating: 5.0, tags: ["Artesanal"] },
];

const categories = [
  { id: "todos", label: "✨ Todos", emoji: "" },
  { id: "buchones", label: "🌹 Buchones", emoji: "" },
  { id: "ramos", label: "💐 Ramos", emoji: "" },
  { id: "cajas", label: "🎁 Cajas", emoji: "" },
  { id: "adicionales", label: "🧸 Adicionales", emoji: "" },
];

export default function ProductGrid() {
  const [active, setActive] = useState("todos");
  const [hoveredId, setHoveredId] = useState<number | null>(null);

  const filtered = active === "todos" ? products : products.filter((p) => p.category === active);

  return (
    <section id="catalogo" style={{ padding: "5rem 1.5rem", maxWidth: 1200, margin: "0 auto" }}>
      {/* Section header */}
      <div style={{ textAlign: "center", marginBottom: "3rem" }}>
        <span style={{ display: "inline-block", background: "linear-gradient(135deg, #fce7f3, #f3e8ff)", padding: "0.4rem 1.2rem", borderRadius: 50, fontSize: "0.85rem", fontWeight: 500, color: "#be185d", marginBottom: "0.8rem" }}>
          💐 Catálogo Día de la Madre
        </span>
        <h2 style={{ fontFamily: "var(--font-heading)", fontSize: "clamp(1.8rem, 4vw, 2.5rem)", fontWeight: 700, color: "#1a1a2e", marginBottom: "0.5rem" }}>
          Nuestros Arreglos Florales
        </h2>
        <p style={{ color: "#64748b", maxWidth: 500, margin: "0 auto", lineHeight: 1.6 }}>
          Cada pieza es creada a mano con limpiapipas de colores. Flores que duran para siempre 🌷
        </p>
      </div>

      {/* Category filter */}
      <div style={{ display: "flex", flexWrap: "wrap", justifyContent: "center", gap: "0.6rem", marginBottom: "2.5rem" }}>
        {categories.map((c) => (
          <button
            key={c.id}
            onClick={() => setActive(c.id)}
            style={{
              padding: "0.6rem 1.4rem",
              borderRadius: 50,
              border: "none",
              cursor: "pointer",
              fontSize: "0.85rem",
              fontWeight: 600,
              transition: "all 0.3s ease",
              background: active === c.id ? "linear-gradient(135deg, #ec4899, #a855f7)" : "#fff",
              color: active === c.id ? "#fff" : "#64748b",
              boxShadow: active === c.id ? "0 4px 15px rgba(236,72,153,0.3)" : "0 2px 8px rgba(0,0,0,0.06)",
            }}
          >
            {c.label}
          </button>
        ))}
      </div>

      {/* Product grid */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))", gap: "1.5rem" }}>
        {filtered.map((p, i) => (
          <div
            key={p.id}
            className="animate-fade-in-up"
            style={{
              animationDelay: `${i * 0.1}s`,
              background: "#fff",
              borderRadius: 20,
              overflow: "hidden",
              boxShadow: hoveredId === p.id ? "0 20px 40px rgba(236,72,153,0.15)" : "0 4px 15px rgba(0,0,0,0.06)",
              transition: "all 0.35s ease",
              transform: hoveredId === p.id ? "translateY(-8px)" : "translateY(0)",
              cursor: "pointer",
            }}
            onMouseEnter={() => setHoveredId(p.id)}
            onMouseLeave={() => setHoveredId(null)}
          >
            {/* Image */}
            <div style={{ position: "relative", overflow: "hidden", height: 240 }}>
              <Image
                src={p.image}
                alt={p.name}
                fill
                sizes="(max-width: 768px) 100vw, 33vw"
                style={{
                  objectFit: "cover",
                  transition: "transform 0.5s ease",
                  transform: hoveredId === p.id ? "scale(1.08)" : "scale(1)",
                }}
              />
              {/* Tags */}
              <div style={{ position: "absolute", top: 12, left: 12, display: "flex", gap: 6 }}>
                {p.tags.map((t) => (
                  <span key={t} style={{ background: "rgba(255,255,255,0.9)", backdropFilter: "blur(8px)", padding: "0.25rem 0.7rem", borderRadius: 50, fontSize: "0.7rem", fontWeight: 600, color: "#be185d" }}>{t}</span>
                ))}
              </div>
            </div>

            {/* Content */}
            <div style={{ padding: "1.2rem 1.4rem" }}>
              <h3 style={{ fontFamily: "var(--font-heading)", fontSize: "1.15rem", fontWeight: 600, color: "#1a1a2e", marginBottom: 6 }}>{p.name}</h3>
              <p style={{ fontSize: "0.82rem", color: "#64748b", lineHeight: 1.5, marginBottom: 10 }}>{p.description}</p>
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
                  <span style={{ color: "#fbbf24", fontSize: "0.9rem" }}>{"★".repeat(Math.floor(p.rating))}</span>
                  <span style={{ fontSize: "0.75rem", color: "#94a3b8" }}>({p.rating})</span>
                </div>
                <span style={{ fontSize: "0.75rem", color: "#a855f7", fontWeight: 600 }}>Cotizar →</span>
              </div>
            </div>

            {/* WhatsApp button on hover */}
            <div style={{
              padding: "0 1.4rem 1.2rem",
              opacity: hoveredId === p.id ? 1 : 0,
              maxHeight: hoveredId === p.id ? 60 : 0,
              transition: "all 0.3s ease",
              overflow: "hidden",
            }}>
              <a
                href={`https://wa.me/51935153080?text=Hola%20me%20interesa%20el%20arreglo%20"${encodeURIComponent(p.name)}"%20💐`}
                target="_blank"
                rel="noopener noreferrer"
                style={{
                  display: "block",
                  textAlign: "center",
                  background: "linear-gradient(135deg, #25d366, #128c7e)",
                  color: "#fff",
                  padding: "0.65rem",
                  borderRadius: 12,
                  fontWeight: 600,
                  fontSize: "0.85rem",
                  textDecoration: "none",
                  transition: "box-shadow 0.3s",
                }}
              >
                💬 Pedir por WhatsApp
              </a>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
