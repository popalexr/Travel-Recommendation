<script setup>
import { ref } from "vue";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@/components/ui/tabs";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Alert,
  AlertDescription,
  AlertTitle,
} from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import {
  ArrowRight,
  LockKeyhole,
  Mail,
  ShieldCheck,
  UserPlus,
} from "lucide-vue-next";

const rememberMe = ref(true);
</script>

<template>
  <div
    class="relative grid min-h-screen place-items-center overflow-hidden bg-gradient-to-br from-background via-background to-primary/20 px-4 py-12 text-foreground"
  >
    <div
      class="pointer-events-none absolute inset-x-0 top-0 h-40 bg-gradient-to-b from-primary/30 via-primary/10 to-transparent blur-3xl"
    />
    <div class="absolute inset-0 -z-10 auth-radial" />

    <div class="w-full max-w-md">
      <div class="mb-8 flex flex-col items-center gap-2 text-center">
        <Badge variant="secondary" class="border border-primary/20 bg-primary/10 text-primary">
          Travel Compass Concierge
        </Badge>
        <h1 class="text-3xl font-semibold tracking-tight sm:text-4xl">
          Welcome back, traveler
        </h1>
        <p class="max-w-sm text-sm text-muted-foreground">
          Sign in to orchestrate your next journey or create an account to unlock curated itineraries, saved collections, and real-time concierge support.
        </p>
      </div>

      <Card class="border border-border/70 bg-background/80 shadow-2xl backdrop-blur">
        <CardHeader class="space-y-3">
          <div class="flex items-center gap-2 text-xs uppercase tracking-[0.3em] text-muted-foreground">
            <ShieldCheck class="size-4" />
            Secure access portal
          </div>
          <CardTitle class="text-2xl">Travel Compass Account</CardTitle>
          <CardDescription class="text-sm text-muted-foreground">
            Use your account email to sync itineraries, invites, and concierge chat.
          </CardDescription>
        </CardHeader>

        <CardContent class="space-y-6">
          <Alert variant="default" class="border-primary/30 bg-primary/5 text-sm">
            <ShieldCheck class="size-4" />
            <div>
              <AlertTitle>Beta Access</AlertTitle>
              <AlertDescription>
                We are onboarding travelers in waves. New accounts receive a complimentary itinerary design session.
              </AlertDescription>
            </div>
          </Alert>

          <Tabs default-value="login" class="w-full">
            <TabsList class="grid w-full grid-cols-2">
              <TabsTrigger value="login" class="text-sm">
                <span class="flex items-center gap-2">
                  <LockKeyhole class="size-4" />
                  Sign In
                </span>
              </TabsTrigger>
              <TabsTrigger value="register" class="text-sm">
                <span class="flex items-center gap-2">
                  <UserPlus class="size-4" />
                  Create Account
                </span>
              </TabsTrigger>
            </TabsList>

            <TabsContent value="login" class="space-y-5 pt-4">
              <form class="space-y-4">
                <div class="space-y-2">
                  <Label for="login-email">Email</Label>
                  <div class="relative">
                    <Mail class="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                    <Input
                      id="login-email"
                      type="email"
                      placeholder="you@journeys.com"
                      class="pl-10"
                    />
                  </div>
                </div>
                <div class="space-y-2">
                  <Label for="login-password">Password</Label>
                  <div class="relative">
                    <LockKeyhole class="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                    <Input
                      id="login-password"
                      type="password"
                      placeholder="••••••••"
                      class="pl-10"
                    />
                  </div>
                </div>
                <div class="flex items-center justify-between text-sm">
                  <label class="flex items-center gap-2 text-muted-foreground">
                    <Checkbox
                      id="remember"
                      v-model="rememberMe"
                      class="mt-0.5"
                    />
                    Keep me signed in
                  </label>
                  <Button variant="link" size="sm" class="px-0 text-primary">
                    Forgot password?
                  </Button>
                </div>
                <Button type="submit" class="w-full gap-2">
                  Continue
                  <ArrowRight class="size-4" />
                </Button>
              </form>
              <p class="text-center text-xs text-muted-foreground">
                By continuing you agree to our
                <a href="#" class="font-medium text-primary hover:underline">Terms of Service</a>
                and
                <a href="#" class="font-medium text-primary hover:underline">Privacy Policy</a>.
              </p>
            </TabsContent>

            <TabsContent value="register" class="space-y-5 pt-4">
              <form class="space-y-4">
                <div class="grid gap-4 sm:grid-cols-2">
                  <div class="space-y-2">
                    <Label for="register-first">First name</Label>
                    <Input id="register-first" placeholder="Avery" autocomplete="given-name" />
                  </div>
                  <div class="space-y-2">
                    <Label for="register-last">Last name</Label>
                    <Input id="register-last" placeholder="Stone" autocomplete="family-name" />
                  </div>
                </div>
                <div class="space-y-2">
                  <Label for="register-email">Email</Label>
                  <Input id="register-email" type="email" placeholder="you@journeys.com" autocomplete="email" />
                </div>
                <div class="space-y-2">
                  <Label for="register-password">Create password</Label>
                  <Input id="register-password" type="password" placeholder="At least 8 characters" autocomplete="new-password" />
                </div>
                <div class="space-y-2">
                  <Label for="register-travel-style">Preferred travel style</Label>
                  <Input id="register-travel-style" placeholder="Ex: Culinary discoveries, hidden nature retreats" />
                  <p class="text-xs text-muted-foreground">
                    We will use this to seed a welcome itinerary inside your dashboard.
                  </p>
                </div>
                <Button type="submit" class="w-full gap-2">
                  Create my account
                  <ArrowRight class="size-4" />
                </Button>
              </form>
              <p class="text-center text-xs text-muted-foreground">
                We respect your inbox. Expect thoughtful trip inspiration, never spam.
              </p>
            </TabsContent>
          </Tabs>
        </CardContent>

        <CardFooter class="flex flex-col gap-3 text-center text-xs text-muted-foreground">
          <p>
            Need concierge access for your team or family?
            <Button variant="link" size="sm" class="px-1 text-sm text-primary">
              Schedule a consult
            </Button>
          </p>
        </CardFooter>
      </Card>
    </div>
  </div>
</template>

<style scoped>
.auth-radial {
  background: radial-gradient(circle at top, hsl(var(--primary) / 0.25), transparent 60%);
}
</style>
