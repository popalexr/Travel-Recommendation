<script setup>
import { reactive, ref, watch } from "vue";
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
import {
  ArrowRight,
  LockKeyhole,
  Mail,
  UserPlus,
} from "lucide-vue-next";

const loginForm = reactive({
  email: "",
  password: "",
  remember: true,
});

const registerForm = reactive({
  firstName: "",
  lastName: "",
  email: "",
  password: "",
});

const loginErrors = reactive({
  general: "",
  fields: {},
});

const registerErrors = reactive({
  general: "",
  fields: {},
});

const isSubmitting = ref(false);


const props = defineProps({
  initialTab: {
    type: String,
    default: "login",
  },
});

const normalizeTab = (value) =>
  value === "register" ? "register" : "login";

const activeTab = ref(normalizeTab(props.initialTab));

function resetErrors(bag) {
  bag.general = "";
  bag.fields = {};
}

function applyErrors(bag, payload) {
  bag.general = payload?.message ?? "Unable to process your request.";
  const details = payload?.errors;
  if (details && typeof details === "object" && Object.keys(details).length > 0) {
    bag.fields = { ...details };
  } else {
    bag.fields = {};
  }
}

async function submitLogin() {
  if (isSubmitting.value) return;
  isSubmitting.value = true;
  resetErrors(loginErrors);
  try {
    const response = await fetch("/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "same-origin",
      body: JSON.stringify({
        email: loginForm.email,
        password: loginForm.password,
        remember: loginForm.remember,
      }),
    });

    const payload = await response.json().catch(() => ({}));

    if (!response.ok) {
      applyErrors(loginErrors, payload);
      return;
    }

    window.location.href = "/dashboard";
  } catch (error) {
    loginErrors.general = "Unexpected error. Please try again.";
  } finally {
    isSubmitting.value = false;
  }
}

async function submitRegister() {
  if (isSubmitting.value) return;
  isSubmitting.value = true;
  resetErrors(registerErrors);
  try {
    const response = await fetch("/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "same-origin",
      body: JSON.stringify({
        firstName: registerForm.firstName,
        lastName: registerForm.lastName,
        email: registerForm.email,
        password: registerForm.password,
      }),
    });

    const payload = await response.json().catch(() => ({}));

    if (!response.ok) {
      applyErrors(registerErrors, payload);
      return;
    }

    window.location.href = "/dashboard";
  } catch (error) {
    registerErrors.general = "Unexpected error. Please try again.";
  } finally {
    isSubmitting.value = false;
  }
}

watch(
  () => props.initialTab,
  (value) => {
    activeTab.value = normalizeTab(value);
  },
);

watch(activeTab, (value) => {
  if (value === "login") {
    resetErrors(loginErrors);
  } else {
    resetErrors(registerErrors);
  }
});
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

        <CardContent class="space-y-6">
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
              <form class="space-y-4" @submit.prevent="submitLogin">
                <div
                  v-if="loginErrors.general"
                  class="rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive"
                >
                  {{ loginErrors.general }}
                </div>
                <div class="space-y-2">
                  <Label for="login-email">Email</Label>
                  <div class="relative">
                    <Mail class="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                    <Input
                      id="login-email"
                      type="email"
                      placeholder="you@journeys.com"
                      v-model="loginForm.email"
                      class="pl-10"
                      :aria-invalid="Boolean(loginErrors.fields.email)"
                    />
                  </div>
                  <p v-if="loginErrors.fields.email" class="text-xs text-destructive">
                    {{ loginErrors.fields.email }}
                  </p>
                </div>
                <div class="space-y-2">
                  <Label for="login-password">Password</Label>
                  <div class="relative">
                    <LockKeyhole class="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                    <Input
                      id="login-password"
                      type="password"
                      placeholder="********"
                      v-model="loginForm.password"
                      class="pl-10"
                      :aria-invalid="Boolean(loginErrors.fields.password)"
                    />
                  </div>
                  <p v-if="loginErrors.fields.password" class="text-xs text-destructive">
                    {{ loginErrors.fields.password }}
                  </p>
                </div>
                <div class="flex items-center justify-between text-sm">
                  <label class="flex items-center gap-2 text-muted-foreground">
                    <Checkbox
                      id="remember"
                      v-model="loginForm.remember"
                      class="mt-0.5"
                    />
                    Keep me signed in
                  </label>
                  <Button variant="link" size="sm" class="px-0 text-primary">
                    Forgot password?
                  </Button>
                </div>
                <Button type="submit" class="w-full gap-2" :disabled="isSubmitting">
                  Continue
                  <ArrowRight class="size-4" />
                </Button>
              </form>
            </TabsContent>

            <TabsContent value="register" class="space-y-5 pt-4">
              <form class="space-y-4" @submit.prevent="submitRegister">
                <div
                  v-if="registerErrors.general"
                  class="rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive"
                >
                  {{ registerErrors.general }}
                </div>
                <div class="grid gap-4 sm:grid-cols-2">
                  <div class="space-y-2">
                    <Label for="register-first">First name</Label>
                    <Input
                      id="register-first"
                      placeholder="Avery"
                      autocomplete="given-name"
                      v-model="registerForm.firstName"
                      :aria-invalid="Boolean(registerErrors.fields.firstName)"
                    />
                    <p v-if="registerErrors.fields.firstName" class="text-xs text-destructive">
                      {{ registerErrors.fields.firstName }}
                    </p>
                  </div>
                  <div class="space-y-2">
                    <Label for="register-last">Last name</Label>
                    <Input
                      id="register-last"
                      placeholder="Stone"
                      autocomplete="family-name"
                      v-model="registerForm.lastName"
                      :aria-invalid="Boolean(registerErrors.fields.lastName)"
                    />
                    <p v-if="registerErrors.fields.lastName" class="text-xs text-destructive">
                      {{ registerErrors.fields.lastName }}
                    </p>
                  </div>
                </div>
                <div class="space-y-2">
                  <Label for="register-email">Email</Label>
                  <Input
                    id="register-email"
                    type="email"
                    placeholder="you@journeys.com"
                    autocomplete="email"
                    v-model="registerForm.email"
                    :aria-invalid="Boolean(registerErrors.fields.email)"
                  />
                  <p v-if="registerErrors.fields.email" class="text-xs text-destructive">
                    {{ registerErrors.fields.email }}
                  </p>
                </div>
                <div class="space-y-2">
                  <Label for="register-password">Create password</Label>
                  <Input
                    id="register-password"
                    type="password"
                    placeholder="At least 8 characters"
                    autocomplete="new-password"
                    v-model="registerForm.password"
                    :aria-invalid="Boolean(registerErrors.fields.password)"
                  />
                  <p v-if="registerErrors.fields.password" class="text-xs text-destructive">
                    {{ registerErrors.fields.password }}
                  </p>
                </div>
                <Button type="submit" class="w-full gap-2" :disabled="isSubmitting">
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







