import type { Metadata } from "next";
import { headers } from "next/headers";
import "./globals.css";

export async function generateMetadata(): Promise<Metadata> {
  const requestHeaders = await headers();
  const host =
    requestHeaders.get("x-forwarded-host") ??
    requestHeaders.get("host") ??
    "localhost:3000";
  const protocol =
    requestHeaders.get("x-forwarded-proto") ??
    (host.startsWith("localhost") ? "http" : "https");
  const ogImageUrl = `${protocol}://${host}/og.png`;

  return {
    title: "Enlighten | Local-first Android study companion",
    description:
      "Capture study material, hear it with live highlighting, understand it with private local AI, and practice with flashcards, quizzes, and Ask Tutor.",
    icons: {
      icon: "/enlighten-hero.png",
      shortcut: "/enlighten-hero.png",
    },
    openGraph: {
      title: "Enlighten",
      description:
        "Turn the material in front of you into something you can hear, understand, and study.",
      type: "website",
      images: [
        {
          url: ogImageUrl,
          width: 1730,
          height: 909,
          alt: "Enlighten: Hear it. Understand it. Practice it.",
        },
      ],
    },
    twitter: {
      card: "summary_large_image",
      title: "Enlighten",
      description:
        "Hear, understand, and practice study material with private local AI.",
      images: [ogImageUrl],
    },
  };
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
