import Navbar from "@/components/Navbar";
import HeroSection from "@/components/HeroSection";
import ProductGrid from "@/components/ProductGrid";
import AboutSection from "@/components/AboutSection";
import HowToOrder from "@/components/HowToOrder";
import Footer from "@/components/Footer";
import Chatbot from "@/components/Chatbot";

export default function Home() {
  return (
    <>
      <Navbar />
      <HeroSection />
      <ProductGrid />
      <AboutSection />
      <HowToOrder />
      <Footer />
      <Chatbot />
    </>
  );
}
