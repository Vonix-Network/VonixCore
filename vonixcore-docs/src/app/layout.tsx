import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import { Header } from "@/components/header";
import { Sidebar } from "@/components/sidebar";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "VonixCore Documentation",
  description: "Comprehensive documentation for VonixCore - All-in-one Minecraft essentials mod/plugin for NeoForge, Paper, and Bukkit.",
  keywords: ["VonixCore", "Minecraft", "mod", "plugin", "NeoForge", "Paper", "Bukkit", "essentials"],
  authors: [{ name: "Vonix Network", url: "https://vonix.network" }],
  openGraph: {
    title: "VonixCore Documentation",
    description: "Comprehensive documentation for VonixCore - All-in-one Minecraft essentials mod/plugin.",
    url: "https://docs.vonix.network",
    siteName: "VonixCore Docs",
    type: "website",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className="dark">
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased min-h-screen`}
      >
        <Header />
        <div className="flex min-h-[calc(100vh-4rem)]">
          <Sidebar />
          <main className="flex-1 overflow-auto">
            {children}
          </main>
        </div>
      </body>
    </html>
  );
}
