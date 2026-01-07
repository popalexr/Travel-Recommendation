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
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  ArrowLeft,
  KeyRound,
  LogOut,
  SlidersHorizontal,
  UserCircle,
} from "lucide-vue-next";

const props = defineProps({
  user: {
    type: Object,
    default: () => ({}),
  },
});

const isLoggingOut = ref(false);
const logoutError = ref("");

const profileForm = reactive({
  firstName: "",
  lastName: "",
  email: "",
});
const isProfileSaving = ref(false);
const profileError = ref("");
const profileStatus = ref("");

const passwordForm = reactive({
  currentPassword: "",
  newPassword: "",
  confirmPassword: "",
});
const isPasswordSaving = ref(false);
const passwordError = ref("");
const passwordStatus = ref("");

watch(
  () => props.user,
  (value) => {
    profileForm.firstName = value?.firstName ?? "";
    profileForm.lastName = value?.lastName ?? "";
    profileForm.email = value?.email ?? "";
  },
  { immediate: true },
);

function resetProfileForm() {
  profileForm.firstName = props.user?.firstName ?? "";
  profileForm.lastName = props.user?.lastName ?? "";
  profileForm.email = props.user?.email ?? "";
  profileError.value = "";
  profileStatus.value = "";
}

async function saveProfile() {
  if (isProfileSaving.value) return;
  isProfileSaving.value = true;
  profileError.value = "";
  profileStatus.value = "";
  try {
    const response = await fetch("/api/settings/profile", {
      method: "POST",
      credentials: "same-origin",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        firstName: profileForm.firstName,
        lastName: profileForm.lastName,
      }),
    });
    const payload = await response.json().catch(() => ({}));
    if (!response.ok) {
      profileError.value = payload?.error ?? "Unable to update profile.";
      return;
    }
    const profile = payload?.profile ?? {};
    profileForm.firstName = profile.firstName ?? "";
    profileForm.lastName = profile.lastName ?? "";
    profileForm.email = profile.email ?? profileForm.email;
    profileStatus.value = "Profile updated.";
  } catch (err) {
    profileError.value = "Unexpected error updating profile.";
  } finally {
    isProfileSaving.value = false;
  }
}

async function updatePassword() {
  if (isPasswordSaving.value) return;
  passwordError.value = "";
  passwordStatus.value = "";

  if (!passwordForm.currentPassword || !passwordForm.newPassword) {
    passwordError.value = "Current and new passwords are required.";
    return;
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    passwordError.value = "New passwords do not match.";
    return;
  }

  isPasswordSaving.value = true;
  try {
    const response = await fetch("/api/settings/password", {
      method: "POST",
      credentials: "same-origin",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
      }),
    });
    const payload = await response.json().catch(() => ({}));
    if (!response.ok) {
      passwordError.value = payload?.error ?? "Unable to update password.";
      return;
    }
    passwordForm.currentPassword = "";
    passwordForm.newPassword = "";
    passwordForm.confirmPassword = "";
    passwordStatus.value = payload?.message ?? "Password updated.";
  } catch (err) {
    passwordError.value = "Unexpected error updating password.";
  } finally {
    isPasswordSaving.value = false;
  }
}

async function logout() {
  if (isLoggingOut.value) return;
  logoutError.value = "";
  isLoggingOut.value = true;
  try {
    const response = await fetch("/logout", {
      method: "POST",
      credentials: "same-origin",
    });
    const payload = await response.json().catch(() => ({}));
    if (!response.ok) {
      logoutError.value = payload?.message ?? "Unable to log out.";
      return;
    }
    window.location.href = "/";
  } catch (err) {
    logoutError.value = "Unexpected error. Please try again.";
  } finally {
    isLoggingOut.value = false;
  }
}
</script>

<template>
  <div class="relative min-h-screen overflow-hidden bg-background text-foreground">
    <div
      class="pointer-events-none absolute inset-x-0 top-0 h-44 bg-gradient-to-b from-primary/25 via-primary/10 to-transparent blur-3xl"
    ></div>
    <div
      class="pointer-events-none absolute -bottom-24 right-0 h-56 w-56 rounded-full bg-secondary/30 blur-3xl"
    ></div>

    <header class="relative border-b border-border/60 bg-background/80 backdrop-blur">
      <div
        class="mx-auto flex max-w-6xl flex-col gap-4 px-6 py-6 md:flex-row md:items-center md:justify-between md:px-8"
      >
        <div class="flex items-center gap-4">
          <div
            class="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/10 text-primary"
          >
            <SlidersHorizontal class="size-5" />
          </div>
          <div>
            <p class="text-xs font-semibold uppercase tracking-[0.3em] text-muted-foreground">
              Settings
            </p>
            <h1 class="text-2xl font-semibold tracking-tight sm:text-3xl">
              Account and experience controls
            </h1>
            <p class="text-sm text-muted-foreground">
              Review your profile, security, and trip defaults in one place.
            </p>
          </div>
        </div>
        <div class="flex flex-wrap items-center gap-3">
          <Button variant="outline" size="sm" class="gap-2" as-child>
            <a href="/dashboard">
              <ArrowLeft class="size-4" />
              Back to dashboard
            </a>
          </Button>
          <Button
            variant="ghost"
            size="sm"
            class="gap-2"
            :disabled="isLoggingOut"
            @click="logout"
          >
            <LogOut class="size-4" />
            {{ isLoggingOut ? "Signing out..." : "Sign out" }}
          </Button>
        </div>
      </div>
    </header>

    <main class="relative mx-auto w-full max-w-6xl px-6 py-10 md:px-8">
      <div
        v-if="logoutError"
        class="mb-6 rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive"
      >
        {{ logoutError }}
      </div>

      <div class="grid gap-6 lg:grid-cols-2">
        <section class="space-y-6">
          <Card class="border border-border/70 bg-background/80">
            <CardHeader class="space-y-2">
              <div class="flex items-center gap-2 text-primary">
                <UserCircle class="size-4" />
                <CardTitle class="text-xl">Profile details</CardTitle>
              </div>
              <CardDescription>
                Update your name and verify the email used for authentication.
              </CardDescription>
            </CardHeader>
            <CardContent class="space-y-4">
              <div
                v-if="profileError"
                class="rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive"
              >
                {{ profileError }}
              </div>
              <div
                v-else-if="profileStatus"
                class="rounded-md border border-emerald-500/30 bg-emerald-500/10 px-3 py-2 text-sm text-emerald-600"
              >
                {{ profileStatus }}
              </div>
              <div class="grid gap-4 sm:grid-cols-2">
                <div class="space-y-2">
                  <Label for="first-name">First name</Label>
                  <Input
                    id="first-name"
                    v-model="profileForm.firstName"
                    placeholder="Avery"
                  />
                </div>
                <div class="space-y-2">
                  <Label for="last-name">Last name</Label>
                  <Input
                    id="last-name"
                    v-model="profileForm.lastName"
                    placeholder="Stone"
                  />
                </div>
              </div>
              <div class="space-y-2">
                <Label for="profile-email">Email</Label>
                <Input
                  id="profile-email"
                  type="email"
                  v-model="profileForm.email"
                  disabled
                  class="bg-muted/40"
                />
              </div>
              <div class="flex flex-wrap items-center justify-end gap-3">
                <Button variant="ghost" size="sm" @click="resetProfileForm">
                  Reset
                </Button>
                <Button
                  size="sm"
                  class="gap-2"
                  :disabled="isProfileSaving"
                  @click="saveProfile"
                >
                  {{ isProfileSaving ? "Saving..." : "Save changes" }}
                </Button>
              </div>
            </CardContent>
          </Card>
        </section>

        <section class="space-y-6">
          <Card class="border border-border/70 bg-background/80">
            <CardHeader class="space-y-2">
              <div class="flex items-center gap-2 text-primary">
                <KeyRound class="size-4" />
                <CardTitle class="text-xl">Security</CardTitle>
              </div>
              <CardDescription>
                Change your password to keep access secure.
              </CardDescription>
            </CardHeader>
            <CardContent class="space-y-4">
              <div
                v-if="passwordError"
                class="rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive"
              >
                {{ passwordError }}
              </div>
              <div
                v-else-if="passwordStatus"
                class="rounded-md border border-emerald-500/30 bg-emerald-500/10 px-3 py-2 text-sm text-emerald-600"
              >
                {{ passwordStatus }}
              </div>
              <div class="space-y-2">
                <Label for="current-password">Current password</Label>
                <Input
                  id="current-password"
                  type="password"
                  v-model="passwordForm.currentPassword"
                  placeholder="Enter current password"
                />
              </div>
              <div class="grid gap-4 sm:grid-cols-2">
                <div class="space-y-2">
                  <Label for="new-password">New password</Label>
                  <Input
                    id="new-password"
                    type="password"
                    v-model="passwordForm.newPassword"
                    placeholder="At least 8 characters"
                  />
                </div>
                <div class="space-y-2">
                  <Label for="confirm-password">Confirm new password</Label>
                  <Input
                    id="confirm-password"
                    type="password"
                    v-model="passwordForm.confirmPassword"
                    placeholder="Repeat new password"
                  />
                </div>
              </div>
              <div class="flex flex-wrap items-center justify-between gap-3 text-xs text-muted-foreground">
                <div class="flex items-center gap-2">
                  <ShieldCheck class="size-4 text-primary" />
                  Use a mix of letters, numbers, and symbols.
                </div>
                <Button
                  size="sm"
                  class="gap-2"
                  :disabled="isPasswordSaving"
                  @click="updatePassword"
                >
                  {{ isPasswordSaving ? "Updating..." : "Update password" }}
                </Button>
              </div>
            </CardContent>
          </Card>
        </section>
      </div>
    </main>
  </div>
</template>
