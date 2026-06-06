import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "YYDetalles con Amor 💐 | Arreglos Florales Artesanales",
  description: "Arreglos florales artesanales hechos con limpiapipas. Ramos buchones, bases decorativas y detalles personalizados para el Día de la Madre. Envíos a todo el Perú.",
  keywords: "flores artesanales, limpiapipas, día de la madre, ramos buchones, arreglos florales, regalo mamá, Perú, Arequipa",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="es" className="h-full antialiased scroll-smooth">
      <body className="min-h-full flex flex-col">{children}</body>
    </html>
  );
}
