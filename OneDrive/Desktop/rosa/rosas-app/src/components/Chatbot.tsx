"use client";
import { useState, useRef, useEffect } from "react";

type Message = { role: "bot" | "user"; text: string };

const INITIAL: Message = {
  role: "bot",
  text: "¡Hola! 🌷✨ Bienvenido/a a **YYDetalles con Amor** 💐\n\nSoy tu asistente y estoy aquí para ayudarte a encontrar el regalo perfecto para mamá.\n\n¿Estás buscando algo para el **Día de la Madre**? ¿Ya tienes algo en mente o te gustaría explorar nuestro catálogo? 🌸",
};

function getBotResponse(msg: string): string {
  const m = msg.toLowerCase();
  if (m.includes("catálogo") || m.includes("catalogo") || m.includes("productos") || m.includes("qué tienen") || m.includes("que tienen") || m.includes("ver"))
    return "¡Claro! 💐 Tenemos estas categorías especiales:\n\n🌹 **Ramos Buchones** — grandes, abundantes y llamativos\n💐 **Ramos / Bases** — desde clásicos hasta bases decorativas\n🧸 **Ramos con Peluche** — incluyen ositos y detalles extra\n🎁 **Cajas decorativas** — presentación premium\n🍫 **Adicionales** — peluches, chocolates, mariposas\n\nTodas nuestras flores son **hechas a mano con limpiapipas** de colores 🌷✨\n\n¿Cuál te llama la atención?";
  if (m.includes("buchón") || m.includes("buchon"))
    return "¡Los ramos buchones son nuestros favoritos! 🌹✨ Son ramos **grandes, abundantes y súper llamativos**. Ideales para impresionar a mamá.\n\nPuedes añadir peluches 🧸, mariposas 🦋 y chocolates 🍫 para hacerlo aún más especial.\n\n¿Te gustaría pedir uno? Te comparto los pasos 💐";
  if (m.includes("precio") || m.includes("cuánto") || m.includes("cuanto") || m.includes("costo") || m.includes("cotiz"))
    return "Los precios varían según el diseño, tamaño y los adicionales que elijas 🎨\n\nPara darte una **cotización exacta**, escríbenos por WhatsApp:\n📱 **935 153 080** o **908 554 594**\n\nTambién puedes enviar una **imagen de referencia** y te cotizamos sin compromiso ✨";
  if (m.includes("pedir") || m.includes("comprar") || m.includes("quiero") || m.includes("pasos") || m.includes("cómo") || m.includes("como pido"))
    return "¡Perfecto! Hacer tu pedido es muy fácil 🎀\n\n**Sigue estos pasos:**\n1️⃣ Escoge tu arreglo favorito del catálogo\n2️⃣ Escríbenos al WhatsApp: **935 153 080** o **908 554 594**\n3️⃣ Envíanos tus datos (nombre, dirección) + fecha de entrega\n4️⃣ Si tienes imagen de referencia, ¡envíala!\n\n⏰ Los pedidos se realizan con **3 a 5 días de anticipación**\n\nHaz feliz a mamá con un detalle hecho con amor ✨❤️💐";
  if (m.includes("envío") || m.includes("envio") || m.includes("delivery") || m.includes("entrega"))
    return "📦 ¡Hacemos envíos a **todo el Perú** mediante Shalom!\n\nEn **Arequipa** tenemos puntos de entrega:\n📍 El Avelino\n📍 Mall Porongoche\n📍 Cementerio Apacheta\n\nLos envíos a alrededores varían de costo según la distancia 🚚\n\n¿Deseas más información? ¡Escríbenos al WhatsApp! 💬";
  if (m.includes("personaliz") || m.includes("color") || m.includes("imagen") || m.includes("referencia"))
    return "¡Claro que sí! 🎨 Puedes **personalizar** tu arreglo:\n\n🌈 Elige los colores que prefieras\n🧸 Agrega peluches\n🦋 Incluye mariposas decorativas\n🍫 Añade chocolates y dulces\n\nSi tienes una **imagen de referencia**, envíala por WhatsApp y te cotizamos sin compromiso ✨\n\n📱 **935 153 080** | **908 554 594**";
  if (m.includes("contacto") || m.includes("whatsapp") || m.includes("teléfono") || m.includes("telefono") || m.includes("número") || m.includes("numero"))
    return "📱 ¡Con gusto! Puedes contactarnos por:\n\n💬 **WhatsApp/Llamadas:**\n• 935 153 080\n• 908 554 594\n\n📸 **Instagram:** @yydetallesconamor10\n\n¡Te atendemos con mucho cariño! 🌷✨";
  if (m.includes("hola") || m.includes("buenas") || m.includes("buen día") || m.includes("hi") || m.includes("hey"))
    return "¡Hola! 🌷✨ ¡Qué gusto saludarte!\n\nSoy tu asistente de **YYDetalles con Amor** 💐\n\n¿En qué puedo ayudarte hoy? ¿Buscas algo especial para el **Día de la Madre**? 🎀";
  if (m.includes("gracias") || m.includes("thanks"))
    return "¡De nada! 💕 Ha sido un placer ayudarte.\n\nRecuerda que puedes escribirnos en cualquier momento 🌷\n\nHaz feliz a mamá con un detalle hecho con amor ✨❤️💐\n\n📱 **935 153 080** | **908 554 594**";
  if (m.includes("material") || m.includes("limpiapipa") || m.includes("pipe"))
    return "🌸 ¡Todas nuestras flores son **hechas a mano con limpiapipas de colores**!\n\nSon suaves, tienen textura aterciopelada y lo mejor: **duran para siempre** 💎\n\nCada pétalo es moldeado con dedicación, creando rosas, tulipanes, margaritas y girasoles 🌻\n\nPuedes añadir:\n🧸 Peluches\n🦋 Mariposas\n🍫 Chocolates";
  return "¡Gracias por tu interés! 💐 Para que pueda ayudarte mejor, puedes preguntarme sobre:\n\n🌹 Nuestros **productos** y catálogo\n💰 **Precios** y cotizaciones\n🛒 **Cómo hacer** tu pedido\n📦 **Envíos** y entregas\n🎨 **Personalización**\n📱 **Contacto** y WhatsApp\n\n¡Estoy para ayudarte! ✨";
}

function renderMarkdown(text: string) {
  const parts = text.split(/(\*\*[^*]+\*\*)/g);
  return parts.map((part, i) => {
    if (part.startsWith("**") && part.endsWith("**")) {
      return <strong key={i} style={{ fontWeight: 600 }}>{part.slice(2, -2)}</strong>;
    }
    return <span key={i}>{part}</span>;
  });
}

export default function Chatbot() {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState<Message[]>([INITIAL]);
  const [input, setInput] = useState("");
  const [typing, setTyping] = useState(false);
  const endRef = useRef<HTMLDivElement>(null);

  useEffect(() => { endRef.current?.scrollIntoView({ behavior: "smooth" }); }, [messages, typing]);

  const send = () => {
    if (!input.trim()) return;
    const userMsg = input.trim();
    setMessages((prev) => [...prev, { role: "user", text: userMsg }]);
    setInput("");
    setTyping(true);
    setTimeout(() => {
      setMessages((prev) => [...prev, { role: "bot", text: getBotResponse(userMsg) }]);
      setTyping(false);
    }, 800 + Math.random() * 600);
  };

  return (
    <>
      {/* FAB */}
      <button
        id="chat-fab"
        onClick={() => setOpen(!open)}
        className="animate-pulse-glow"
        style={{
          position: "fixed", bottom: 24, right: 24, zIndex: 9999,
          width: 60, height: 60, borderRadius: "50%",
          background: "linear-gradient(135deg, #ec4899, #a855f7)",
          border: "none", cursor: "pointer", display: "flex",
          alignItems: "center", justifyContent: "center",
          boxShadow: "0 8px 25px rgba(236,72,153,0.4)",
          transition: "transform 0.3s",
          transform: open ? "rotate(45deg)" : "rotate(0)",
          fontSize: "1.6rem", color: "#fff",
        }}
        aria-label="Abrir chat de ventas"
      >{open ? "✕" : "💬"}</button>

      {/* Chat window */}
      {open && (
        <div
          className="animate-slide-down"
          style={{
            position: "fixed", bottom: 96, right: 24, zIndex: 9998,
            width: "min(380px, calc(100vw - 48px))",
            height: "min(520px, calc(100vh - 140px))",
            background: "#fff",
            borderRadius: 20,
            boxShadow: "0 25px 60px rgba(0,0,0,0.15)",
            display: "flex", flexDirection: "column",
            overflow: "hidden",
            border: "1px solid rgba(236,72,153,0.12)",
          }}
        >
          {/* Header */}
          <div style={{
            background: "linear-gradient(135deg, #ec4899, #a855f7)",
            padding: "1rem 1.2rem",
            display: "flex", alignItems: "center", gap: "0.75rem",
          }}>
            <div style={{ width: 40, height: 40, borderRadius: "50%", background: "rgba(255,255,255,0.2)", display: "flex", alignItems: "center", justifyContent: "center", fontSize: "1.3rem" }}>🌷</div>
            <div>
              <div style={{ color: "#fff", fontWeight: 700, fontSize: "0.95rem" }}>YYDetalles con Amor</div>
              <div style={{ color: "rgba(255,255,255,0.8)", fontSize: "0.72rem" }}>🟢 En línea · Responde al instante</div>
            </div>
          </div>

          {/* Messages */}
          <div style={{ flex: 1, overflowY: "auto", padding: "1rem", display: "flex", flexDirection: "column", gap: "0.75rem", background: "#fdf2f8" }}>
            {messages.map((m, i) => (
              <div key={i} style={{ display: "flex", justifyContent: m.role === "user" ? "flex-end" : "flex-start" }}>
                <div style={{
                  maxWidth: "85%",
                  padding: "0.75rem 1rem",
                  borderRadius: m.role === "user" ? "16px 16px 4px 16px" : "16px 16px 16px 4px",
                  background: m.role === "user" ? "linear-gradient(135deg, #ec4899, #a855f7)" : "#fff",
                  color: m.role === "user" ? "#fff" : "#1a1a2e",
                  fontSize: "0.85rem",
                  lineHeight: 1.55,
                  boxShadow: "0 2px 8px rgba(0,0,0,0.06)",
                  whiteSpace: "pre-line",
                }}>
                  {renderMarkdown(m.text)}
                </div>
              </div>
            ))}
            {typing && (
              <div style={{ display: "flex", justifyContent: "flex-start" }}>
                <div style={{ background: "#fff", padding: "0.75rem 1rem", borderRadius: "16px 16px 16px 4px", boxShadow: "0 2px 8px rgba(0,0,0,0.06)", fontSize: "1.2rem", letterSpacing: 4 }}>
                  <span className="animate-bounce-slow" style={{ display: "inline-block" }}>•</span>
                  <span className="animate-bounce-slow delay-200" style={{ display: "inline-block" }}>•</span>
                  <span className="animate-bounce-slow delay-400" style={{ display: "inline-block" }}>•</span>
                </div>
              </div>
            )}
            <div ref={endRef} />
          </div>

          {/* Quick actions */}
          <div style={{ padding: "0.5rem 1rem 0", display: "flex", gap: 6, flexWrap: "wrap", background: "#fff" }}>
            {["Catálogo", "Precios", "Cómo pedir", "Envíos"].map((q) => (
              <button key={q} onClick={() => { setInput(q); }}
                style={{ padding: "0.35rem 0.8rem", borderRadius: 50, border: "1px solid rgba(236,72,153,0.2)", background: "#fdf2f8", color: "#be185d", fontSize: "0.72rem", fontWeight: 500, cursor: "pointer", transition: "all 0.2s" }}
                onMouseEnter={(e) => { e.currentTarget.style.background = "#fce7f3"; }}
                onMouseLeave={(e) => { e.currentTarget.style.background = "#fdf2f8"; }}
              >{q}</button>
            ))}
          </div>

          {/* Input */}
          <div style={{ padding: "0.75rem 1rem", display: "flex", gap: 8, background: "#fff", borderTop: "1px solid rgba(236,72,153,0.08)" }}>
            <input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && send()}
              placeholder="Escribe tu mensaje..."
              style={{ flex: 1, padding: "0.65rem 1rem", borderRadius: 50, border: "1px solid rgba(236,72,153,0.15)", fontSize: "0.85rem", outline: "none", background: "#fdf2f8", transition: "border-color 0.3s" }}
              onFocus={(e) => (e.currentTarget.style.borderColor = "#ec4899")}
              onBlur={(e) => (e.currentTarget.style.borderColor = "rgba(236,72,153,0.15)")}
            />
            <button onClick={send}
              style={{ width: 40, height: 40, borderRadius: "50%", border: "none", background: "linear-gradient(135deg, #ec4899, #a855f7)", color: "#fff", cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center", fontSize: "1.1rem", transition: "transform 0.2s" }}
              onMouseEnter={(e) => (e.currentTarget.style.transform = "scale(1.1)")}
              onMouseLeave={(e) => (e.currentTarget.style.transform = "scale(1)")}
            >➤</button>
          </div>
        </div>
      )}
    </>
  );
}
