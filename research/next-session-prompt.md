# Prompt for the next Kairo session

Paste the block below as the first message of a new session, opened in `~/coding/mobile-applications/current/kairo`.

```
We're continuing the Kairo v2 build (code, not design). The design is on the canvas
https://claude.ai/artifact/XuXAdkKY8P3N7g4Ps5GiHa.

Before anything else:
1. Read research/build-handoff.md ("Start here" first), CLAUDE.md, and your memory notes.
2. Check `git status`: v2 work is uncommitted on main. Don't commit unless I say so.
3. Rules: files ≤300 lines; gradle --max-workers=2; no emulator; install the debug build on my vivo
   (./gradlew :app:installDebug -q --max-workers=2) and check each piece on the phone.

Today: continue Finance with <WRITE HERE — e.g. "Ivy import and the Finance drawer">, then the next
item in the handoff's "Not built yet" list. Read each canvas board before building it, and tell me
what's not at the bar.
```
