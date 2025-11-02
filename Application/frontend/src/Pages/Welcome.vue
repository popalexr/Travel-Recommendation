<script setup>
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import {
  ArrowRight,
  Edit3,
  FileUp,
  Info,
  LockKeyhole,
  RefreshCcw,
  Sparkles,
  Layers,
  UserCheck,
  UserPlus,
  Wand2,
} from "lucide-vue-next";

const pillars = [
  {
    icon: FileUp,
    title: "Document-driven planning",
    description:
      "Upload flight confirmations, lodging invoices, or other travel files and let the platform extract structured trip data without manual typing.",
  },
  {
    icon: Wand2,
    title: "LLM-powered recommendations",
    description:
      "Travel Recommendation collaborates with ChatGPT to craft reliable suggestions while respecting the details and constraints from your documents.",
  },
  {
    icon: Layers,
    title: "Structured itineraries",
    description:
      "Every proposal is organized by day, theme, and location so travelers can instantly act on the plan or adjust it to their needs.",
  },
  {
    icon: UserCheck,
    title: "Account-ready experience",
    description:
      "Authentication keeps trips in sync, enabling each traveler to revisit, refine, and expand their journey planning history.",
  },
];

const workflow = [
  {
    icon: FileUp,
    title: "Ingest travel documents",
    description:
      "Drag and drop airline tickets, accommodation receipts, or PDFs. The parser identifies destinations, dates, and key logistics automatically.",
  },
  {
    icon: Edit3,
    title: "Review extracted details",
    description:
      "Confirm or adjust the detected itinerary outline. Edit traveler preferences to highlight interests, pace, or budget considerations.",
  },
  {
    icon: Sparkles,
    title: "Generate curated itinerary",
    description:
      "The AI concierge assembles day plans, meal ideas, and nearby attractions, rooted in the verified trip context you provided.",
  },
  {
    icon: RefreshCcw,
    title: "Iterate with confidence",
    description:
      "Regenerate routes, add alternatives, or request variations when plans shift. The system keeps context to avoid starting from scratch.",
  },
];

const authOptions = [
  {
    icon: LockKeyhole,
    title: "Login",
    description:
      "Returning travelers access saved itineraries, regenerate suggestions, and pick up where planning left off.",
    action: {
      label: "Go to login",
      href: "/login",
    },
  },
  {
    icon: UserPlus,
    title: "Register",
    description:
      "Create an account to store trip documents securely and personalize every recommendation from day one.",
    action: {
      label: "Create account",
      href: "/register",
    },
  },
];

const commitments = [
  {
    title: "Human-centred design",
    description:
      "Travel Recommendation focuses on clarity and guidance so that every traveler understands the next best step.",
  },
  {
    title: "Scalable foundation",
    description:
      "The chosen stack keeps the codebase maintainable and ready for new channels like mobile or partner portals.",
  },
  {
    title: "Transparency first",
    description:
      "Recommendations clearly describe why a location or activity was suggested, preventing fabricated or irrelevant tips.",
  },
];
</script>

<template>
  <div class="min-h-screen bg-background text-foreground">
    <header
      class="sticky top-0 z-30 border-b border-border/60 bg-background/80 backdrop-blur"
    >
      <div
        class="mx-auto flex max-w-6xl items-center justify-between px-6 py-4 md:px-8"
      >
        <div class="flex items-center gap-3">
          <div
            class="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10 text-primary"
          >
            <Sparkles class="size-5" />
          </div>
          <div class="flex flex-col">
            <span class="text-sm font-semibold uppercase tracking-[0.3em]"
              >Travel Recommendation</span
            >
            <span class="text-xs text-muted-foreground">
              Intelligent itinerary companion
            </span>
          </div>
        </div>

        <nav class="hidden gap-3 text-sm md:flex">
          <Button variant="ghost" class="text-sm" as-child>
            <a href="#pillars">Why it works</a>
          </Button>
          <Button variant="ghost" class="text-sm" as-child>
            <a href="#workflow">How it works</a>
          </Button>
          <Button variant="ghost" class="text-sm" as-child>
            <a href="#auth">Auth</a>
          </Button>
        </nav>

        <Button variant="outline" size="sm" class="md:hidden">
          Menu
        </Button>
      </div>
    </header>

    <main class="mx-auto flex max-w-6xl flex-col gap-20 px-6 py-16 md:px-8">
      <section id="overview" class="grid gap-12 lg:grid-cols-[1.15fr_0.85fr]">
        <div class="space-y-6">
          <Badge
            variant="secondary"
            class="inline-flex items-center gap-2 border border-primary/40 bg-primary/10 text-primary"
          >
            <Sparkles class="size-3.5" />
            Guided by your real travel data
          </Badge>
          <h1 class="text-4xl font-semibold tracking-tight sm:text-5xl">
            Plan every journey with confidence and context.
          </h1>
          <p class="text-lg text-muted-foreground sm:text-xl">
            Travel Recommendation interprets the paperwork you already possess
            and turns it into a personalized, trustworthy itinerary. No guesswork,
            no invented attractions&mdash;just a clear companion for preparing the
            perfect trip.
          </p>
          <ul class="grid gap-3 text-sm text-muted-foreground">
            <li class="flex items-start gap-2">
              <Info class="mt-0.5 size-4 text-primary" />
              Upload tickets, invoices, or notes to seed the itinerary engine.
            </li>
            <li class="flex items-start gap-2">
              <Info class="mt-0.5 size-4 text-primary" />
              Adapt AI suggestions by editing preferences before confirming.
            </li>
            <li class="flex items-start gap-2">
              <Info class="mt-0.5 size-4 text-primary" />
              Retain a structured plan with daily outlines, logistics, and TO DOs.
            </li>
          </ul>

          <div class="flex flex-col gap-3 sm:flex-row">
            <Button size="lg" class="gap-2">
              Start planning
              <ArrowRight class="size-4" />
            </Button>
            <Button variant="outline" size="lg" as-child>
              <a href="#auth">Visit auth options</a>
            </Button>
          </div>
        </div>

        <Card class="border border-border/70 bg-background/70 shadow-lg">
          <CardHeader class="space-y-3">
            <Badge variant="outline" class="w-fit text-xs">
              AI concierge snapshot
            </Badge>
            <CardTitle class="text-2xl">
              From document drop to daily agenda
            </CardTitle>
            <CardDescription>
              A preview of how the system transforms trip proofs into lived
              adventures.
            </CardDescription>
          </CardHeader>
          <CardContent class="space-y-4 text-sm text-muted-foreground">
            <div class="rounded-2xl border border-border/60 bg-background/80 p-4">
              <p class="font-medium text-foreground">
                Amalfi Coast | 5 night stay | June 2025
              </p>
              <Separator class="my-3" />
              <p>
                Arrival day features a sunset walk through Atrani, followed by a
                chef-led tasting at La Bonta del Capo. Day two brings a private
                boat to Capri with curated stops aligned to your time window. Each
                recommendation references the uploaded booking to stay realistic.
              </p>
            </div>
            <p>
              Regenerate ideas, swap activities, or ask for family-friendly
              alternatives. Travel Recommendation keeps the verified travel details
              intact while refreshing the creative layer.
            </p>
          </CardContent>
        </Card>
      </section>

      <section id="pillars" class="space-y-10">
        <div class="flex flex-col gap-3 text-center">
          <Badge variant="secondary" class="mx-auto w-fit bg-secondary/40">
            Why the approach works
          </Badge>
          <h2 class="text-3xl font-semibold tracking-tight sm:text-4xl">
            Built around traveler needs and project goals
          </h2>
          <p class="mx-auto max-w-3xl text-muted-foreground">
            Every requirement outlined in the project documentation is reflected
            in these pillars: a dependable pipeline from document ingestion, to AI
            reasoning, to clear delivery.
          </p>
        </div>
        <div class="grid gap-6 md:grid-cols-2">
          <Card v-for="pillar in pillars" :key="pillar.title">
            <CardHeader class="space-y-3">
              <div
                class="flex size-12 items-center justify-center rounded-xl bg-primary/10 text-primary"
              >
                <component :is="pillar.icon" class="size-5" />
              </div>
              <CardTitle class="text-xl">
                {{ pillar.title }}
              </CardTitle>
            </CardHeader>
            <CardContent class="text-sm text-muted-foreground">
              {{ pillar.description }}
            </CardContent>
          </Card>
        </div>
      </section>

      <section id="workflow" class="space-y-10">
        <div class="flex flex-col gap-3 text-center">
          <Badge variant="secondary" class="mx-auto w-fit bg-secondary/40">
            How it works
          </Badge>
          <h2 class="text-3xl font-semibold tracking-tight sm:text-4xl">
            A reliable workflow from upload to itinerary
          </h2>
          <p class="mx-auto max-w-3xl text-muted-foreground">
            The experience is intentionally simple: provide context, verify the
            details, and let the assistant deliver and iterate on a polished plan.
          </p>
        </div>
        <div class="grid gap-6 lg:grid-cols-2">
          <Card
            v-for="(step, index) in workflow"
            :key="step.title"
            class="relative overflow-hidden"
          >
            <span
              class="absolute right-4 top-4 flex size-9 items-center justify-center rounded-full bg-primary/10 text-sm font-semibold text-primary"
            >
              {{ index + 1 }}
            </span>
            <CardHeader class="space-y-3">
              <div
                class="flex size-12 items-center justify-center rounded-xl bg-primary/10 text-primary"
              >
                <component :is="step.icon" class="size-5" />
              </div>
              <CardTitle class="text-xl">
                {{ step.title }}
              </CardTitle>
            </CardHeader>
            <CardContent class="text-sm text-muted-foreground">
              {{ step.description }}
            </CardContent>
          </Card>
        </div>
      </section>

      <section id="auth" class="space-y-10">
        <div class="flex flex-col gap-3 text-center">
          <Badge variant="secondary" class="mx-auto w-fit bg-secondary/40">
            Auth
          </Badge>
          <h2 class="text-3xl font-semibold tracking-tight sm:text-4xl">
            Secure access tailored for travelers
          </h2>
          <p class="mx-auto max-w-3xl text-muted-foreground">
            Whether you are returning to refine an itinerary or joining the platform
            for the first time, the authentication flow keeps documents protected
            and experiences personal.
          </p>
        </div>

        <div class="grid gap-6 md:grid-cols-2">
          <Card v-for="option in authOptions" :key="option.title">
            <CardHeader class="space-y-3">
              <div
                class="flex size-12 items-center justify-center rounded-xl bg-primary/10 text-primary"
              >
                <component :is="option.icon" class="size-5" />
              </div>
              <CardTitle class="text-xl">
                {{ option.title }}
              </CardTitle>
            </CardHeader>
            <CardContent class="space-y-3 text-sm text-muted-foreground">
              <p>{{ option.description }}</p>
              <Button variant="outline" size="sm" as-child>
                <a :href="option.action.href">
                  {{ option.action.label }}
                </a>
              </Button>
            </CardContent>
          </Card>
        </div>
      </section>

      <section class="space-y-10">
        <div class="flex flex-col gap-3 text-center">
          <Badge variant="secondary" class="mx-auto w-fit bg-secondary/40">
            Our promise
          </Badge>
          <h2 class="text-3xl font-semibold tracking-tight sm:text-4xl">
            A planner that earns traveler trust
          </h2>
        </div>
        <div class="grid gap-6 md:grid-cols-3">
          <Card v-for="item in commitments" :key="item.title">
            <CardHeader>
              <CardTitle class="text-xl">
                {{ item.title }}
              </CardTitle>
            </CardHeader>
            <CardContent class="text-sm text-muted-foreground">
              {{ item.description }}
            </CardContent>
          </Card>
        </div>
        <div
          class="rounded-3xl border border-border/60 bg-gradient-to-br from-background via-primary/5 to-secondary/20 p-10 text-center shadow-inner"
        >
          <h3 class="text-2xl font-semibold tracking-tight sm:text-3xl">
            Ready to turn travel documents into travel memories?
          </h3>
          <p class="mx-auto mt-3 max-w-2xl text-muted-foreground">
            Begin with the essentials, iterate with AI, and watch your itinerary
            take shape. Travel Recommendation guides you from the first upload to
            the final booking checklist.
          </p>
          <div class="mt-6 flex flex-col items-center justify-center gap-3 sm:flex-row">
            <Button size="lg" class="gap-2" as-child>
              <a href="/register">
                Create your first trip
                <ArrowRight class="size-4" />
              </a>
            </Button>
            <Button variant="ghost" size="lg" as-child>
              <a href="/login">Sign in instead</a>
            </Button>
          </div>
        </div>
      </section>
    </main>

    <footer class="border-t border-border/60 bg-background py-10">
      <div
        class="mx-auto flex max-w-6xl flex-col items-center justify-between gap-4 px-6 text-center text-sm text-muted-foreground md:flex-row md:px-8 md:text-left"
      >
        <p>
          &copy; {{ new Date().getFullYear() }} Travel Recommendation. Guided by the
          Travel Recommendation project brief.
        </p>
        <div class="flex flex-wrap justify-center gap-4">
          <a class="hover:text-foreground" href="#workflow">Process</a>
          <a
            class="hover:text-foreground"
            href="/Docs/Documenta%C8%9Bie___Proiectare_Web%20(1).pdf"
          >
            PDF
          </a>
        </div>
      </div>
    </footer>
  </div>
</template>
