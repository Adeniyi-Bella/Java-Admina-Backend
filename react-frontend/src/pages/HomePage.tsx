// @pages/HomePage.tsx

import { ArrowRight } from "lucide-react";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/common/Card/Card";
import * as motion from "motion/react-client";
import { Link } from "@tanstack/react-router";
import { useMsalLoginLogoutSignup } from "@/hooks/msal/useMsalLoginLogoutSignup";
import Footer from "@/components/common/Footer/Footer";
import { features } from "@/components/common/Features/Features";
import { brandStyles } from "@/lib/design/styles";
import { Button } from "@/components/common/Button/Button";

export default function HomePage() {
  const { login, signUp } = useMsalLoginLogoutSignup();

  const renderAnimatedChars = (text: string, delayStart = 0) =>
    text.split("").map((char, i) => (
      <motion.span
        key={i}
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{
          delay: delayStart + i * 0.05,
          duration: 0.3,
        }}
        style={{ display: "inline-block" }}
      >
        {char === " " ? "\u00A0" : char}
      </motion.span>
    ));

  return (
    <>
      <div className={`min-h-screen ${brandStyles.surface}`}>
        <header
          className={`border-b ${brandStyles.surface}`}
          style={{ backdropFilter: "blur(8px)" }}
        >
          <div className="container mx-auto px-4 py-4 flex items-center justify-between">
            <div className="flex items-center gap-10">
              <img
                src="/images/admina-logo.png"
                alt="Admina Logo"
                width={120}
                height={40}
                className="h-[30%] w-[20%]"
              />
              <nav className="hidden sm:flex items-center space-x-4">
                <Link to="/" className="text-gray-600 hover:text-gray-900">
                  Home
                </Link>
                <Link
                  to="/pricing"
                  className="text-gray-600 hover:text-gray-900"
                >
                  Pricing
                </Link>
                {/*
                <Link to="/blog" className="text-gray-600 hover:text-gray-900">
                  Blog
                </Link> */}
              </nav>
            </div>

            <nav className="flex items-center space-x-4">
              <Button
                variant="link"
                className="text-gray-600 hover:text-gray-900"
                onClick={login}
              >
                Log In
              </Button>
              <Button
                variant="link"
                className="text-gray-600 hover:text-gray-900"
                onClick={signUp}
              >
                Sign Up
              </Button>
            </nav>
          </div>
        </header>

        <section className="relative overflow-hidden">
          <div className="container mx-auto px-4 py-24 text-center">
            <div className="max-w-4xl mx-auto">
              <div className="mb-8 transform transition-all" />
              <div className="mb-8 transform transition-all">
                <h1
                  className={`text-5xl md:text-8xl font-fredoka mb-4 tracking-[2px] ${brandStyles.brandAltText}`}
                  style={{ textShadow: "0 0 30px rgba(226, 125, 96, 0.3)" }}
                >
                  ADMINA
                </h1>
              </div>

              <h2 className="text-4xl md:text-5xl text-gray-900 mb-8 leading-tight">
                {renderAnimatedChars("Letter In. Clarity Out.")}
              </h2>

              <p className="text-xl md:text-2xl text-gray-600 mb-12 leading-relaxed max-w-3xl mx-auto">
                We turn complicated documents into clear, useful insights.
                Upload a file (PDF or Image), and get an instant summary, key
                points, action plans, translation, and a smart chat assistant to
                help you understand it better.
              </p>

              <div className="flex flex-col sm:flex-row gap-6 justify-center items-center">
                <Button
                  size="lg"
                  variant="outline"
                  className={`text-xl px-12 py-6 rounded-full border-2 transition-all duration-500 bg-transparent transform hover:scale-110 hover:-translate-y-1 ${brandStyles.brandOutlineButton} ${brandStyles.brandDeepText}`}
                  onClick={signUp}
                >
                  Get Started
                  <ArrowRight className="ml-3 h-6 w-6" />
                </Button>
              </div>
            </div>
          </div>

          <div className="absolute top-0 left-0 w-full h-full overflow-hidden pointer-events-none">
            <div
              className={`absolute top-20 left-10 w-32 h-32 rounded-full opacity-10 ${brandStyles.brandBg}`}
              style={{ animation: "float-random-2 12s ease-in-out infinite" }}
            />
            <div
              className={`absolute bottom-20 right-10 w-48 h-48 rounded-full opacity-5 ${brandStyles.brandBg}`}
              style={{ animation: "float-random-2 12s ease-in-out infinite" }}
            />
            <div
              className={`absolute top-1/2 left-1/4 w-24 h-24 rounded-full opacity-10 ${brandStyles.brandBg}`}
              style={{ animation: "float-random-3 10s ease-in-out infinite" }}
            />
          </div>
        </section>

        <section className="container mx-auto px-4 py-5">
          <h3 className="text-4xl font-bold text-center text-gray-900 mb-16">
            How Admina Works
          </h3>

          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
            {features.map((feature, index) => {
              const Icon = feature.icon;
              return (
                <Card
                  key={index}
                  className={`text-center hover:shadow-lg border-2 transition-shadow duration-300 ${brandStyles.brandCard}`}
                >
                  <CardHeader>
                    <div
                      className={`h-16 w-16 mx-auto mb-6 flex items-center justify-center rounded-2xl ${brandStyles.brandBg}`}
                    >
                      <Icon className="h-8 w-8 text-white" />
                    </div>
                    <CardTitle className="text-xl">{feature.title}</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <CardDescription className="text-base">
                      {feature.description}
                    </CardDescription>
                  </CardContent>
                </Card>
              );
            })}
          </div>
        </section>

        <section className="container mx-auto px-4 py-20 text-center">
          <div className="max-w-3xl mx-auto">
            <h3 className="text-2xl font-bold text-gray-900 mb-8">
              Tired of Confusing Letters? We Can Help.
            </h3>
            <Button
              size="lg"
              className={`text-lg px-10 py-5 rounded-full ${brandStyles.brandButton}`}
              onClick={signUp}
            >
              Start Free
            </Button>
          </div>
        </section>

        <Footer />
      </div>
    </>
  );
}
