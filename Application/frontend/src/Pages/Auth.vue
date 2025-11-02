<script setup>
import { ref, watch } from "vue";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
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
import { Badge } from "@/components/ui/badge";
import {
  ArrowRight,
  LockKeyhole,
  Mail,
  UserPlus,
} from "lucide-vue-next";

const rememberMe = ref(true);

const props = defineProps({
  initialTab: {
    type: String,
    default: "login",
  },
});

const normalizeTab = (value) =>
  value === "register" ? "register" : "login";

const activeTab = ref(normalizeTab(props.initialTab));

watch(
  () => props.initialTab,
  (value) => {
    activeTab.value = normalizeTab(value);
  },
);
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
        <h1 class="text-3xl font-semibold tracking-tight sm:text-4xl">
          Travel Recommendation
        </h1>
        <p class="max-w-sm text-sm text-muted-foreground">
          Sign in or create your account to manage itineraries and saved trips.
        </p>
      </div>

      <Card class="border border-border/70 bg-background/80 shadow-2xl backdrop-blur">
        <CardHeader>
          <CardTitle class="text-2xl">Travel Compass Account</CardTitle>
          <CardDescription class="text-sm text-muted-foreground">
            Use your email to sync itineraries across devices.
          </CardDescription>
        </CardHeader>

        <CardContent>
          <Tabs v-model="activeTab" class="w-full">
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
                      placeholder="********"
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
                <Button type="submit" class="w-full gap-2">
                  Create my account
                  <ArrowRight class="size-4" />
                </Button>
              </form>
            </TabsContent>
          </Tabs>
        </CardContent>
      </Card>
    </div>
  </div>
</template>

<style scoped>
.auth-radial {
  background: radial-gradient(circle at top, hsl(var(--primary) / 0.25), transparent 60%);
}
</style>


