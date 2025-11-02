<script setup>
import { computed, reactive, ref } from "vue";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { LogOut, MapPin } from "lucide-vue-next";

const props = defineProps({
  user: {
    type: Object,
    default: () => ({}),
  },
});

const state = reactive({
  message: "",
  error: "",
});

const isLoggingOut = ref(false);

const displayName = computed(() => {
  const { firstName, lastName, email } = props.user ?? {};
  if (firstName || lastName) {
    return [firstName, lastName].filter(Boolean).join(" ");
  }
  return email ?? "Traveler";
});

async function logout() {
  if (isLoggingOut.value) return;
  state.message = "";
  state.error = "";
  isLoggingOut.value = true;
  try {
    const response = await fetch("/logout", {
      method: "POST",
      credentials: "same-origin",
    });
    const payload = await response.json().catch(() => ({}));
    if (!response.ok) {
      state.error = payload?.message ?? "Unable to log out.";
      return;
    }
    state.message = payload?.message ?? "See you soon!";
    window.location.href = "/";
  } catch (error) {
    state.error = "Unexpected error. Please try again.";
  } finally {
    isLoggingOut.value = false;
  }
}
</script>

<template>
  <div class="min-h-screen bg-background text-foreground">
    <header class="border-b border-border/60 bg-background/80 backdrop-blur">
      <div class="mx-auto flex max-w-5xl items-center justify-between px-6 py-4 md:px-8">
        <div>
          <p class="text-xs uppercase tracking-[0.3em] text-muted-foreground">
            Welcome back
          </p>
          <h1 class="text-2xl font-semibold tracking-tight">
            {{ displayName }}
          </h1>
        </div>
        <Button variant="outline" class="gap-2" @click="logout" :disabled="isLoggingOut">
          <LogOut class="size-4" />
          Logout
        </Button>
      </div>
    </header>

    <main class="mx-auto flex max-w-5xl flex-col gap-8 px-6 py-10 md:px-8">
      <section class="space-y-4">
        <Card>
          <CardHeader>
            <CardTitle>Trip Planning Hub</CardTitle>
            <CardDescription>
              Your personal recommendations, itineraries, and documents in one place.
            </CardDescription>
          </CardHeader>
          <CardContent class="space-y-4">
            <p class="text-sm text-muted-foreground">
              Upload confirmations, organise daily agendas, and regenerate ideas tailored to each journey.
              We keep everything private to your account until you are ready to share.
            </p>
            <Separator />
            <div class="grid gap-4 sm:grid-cols-2">
              <div class="rounded-xl border border-border/70 bg-background/70 p-4">
                <p class="font-medium text-foreground flex items-center gap-2">
                  <MapPin class="size-4 text-primary" />
                  Next steps
                </p>
                <ul class="mt-3 space-y-2 text-sm text-muted-foreground">
                  <li>Upload your latest travel documents.</li>
                  <li>Annotate preferences for each traveler.</li>
                  <li>Generate itineraries with one click.</li>
                </ul>
              </div>
              <div class="rounded-xl border border-border/70 bg-background/70 p-4">
                <p class="font-medium text-foreground">Account snapshot</p>
                <p class="mt-2 text-sm text-muted-foreground">
                  Email: <span class="font-medium text-foreground">{{ props.user?.email }}</span>
                </p>
                <p class="text-xs text-muted-foreground/80">
                  Keep your contact details current so concierge notifications reach you instantly.
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        <div v-if="state.message" class="rounded-md border border-primary/40 bg-primary/10 px-3 py-2 text-sm text-primary">
          {{ state.message }}
        </div>
        <div v-if="state.error" class="rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive">
          {{ state.error }}
        </div>
      </section>
    </main>
  </div>
</template>
