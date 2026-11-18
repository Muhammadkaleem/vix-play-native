# Vix Play — AdMob Revenue Analysis

**Prepared:** 2026-11-18
**Sources:**
- `admob-report.csv`: daily totals, 2026-10-21 → 2026-11-17 (25 days with traffic, 28 calendar days).
- `admob-report (1).csv`: per-ad-unit totals. The file does not say which dates it covers. Its request and impression counts match the daily file's **2026-11-11 → 2026-11-17** rows exactly (152 requests, 43 impressions), so it is treated as that week.

Every number below is computed from those two files. Each finding is marked **[Confirmed]** (read straight from the data or the code) or **[Hypothesis]** (an inference that still needs checking).

---

## 1. Headline numbers

| Metric | Value (21 Oct – 17 Nov) |
|---|---|
| Estimated earnings | **$0.12** reported (≈ $0.145 recomputed as eCPM × impressions; the report rounds each day to the cent) |
| Ad requests | 446 (≈ 17.8 / day) |
| Matched requests | 242 → **match rate 54.3 %** |
| Impressions | 109 (≈ 4.4 / day) → **show rate 45.0 %** of matched |
| Impressions / requests | **24.4 %**: three of every four requests never became a paid impression |
| Clicks | 9 → **CTR 8.3 %** |
| Effective eCPM | ≈ **$1.33** |
| Days with traffic but zero impressions | 22 Oct, 28 Oct, 6 Nov, 10 Nov, 17 Nov |
| Days with no traffic at all | 29 Oct, 8 Nov, 14 Nov (no row) |

### Weekly trend [Confirmed]

| ISO week | Days | Requests | Match rate | Impressions | Est. earnings |
|---|---|---|---|---|---|
| 43 (21–25 Oct) | 5 | 89 | 86.5 % | 20 | $0.015 |
| 44 (26 Oct–1 Nov) | 6 | 145 | **25.5 %** | 18 | $0.010 |
| 45 (2–7 Nov) | 6 | 54 | 77.8 % | 24 | $0.017 |
| 46 (9–15 Nov) | 6 | 91 | 60.4 % | 29 | $0.055 |
| 47 (16–17 Nov) | 2 | 67 | 46.3 % | 18 | $0.048 |

Revenue rose in weeks 46–47. That rise lines up with the rewarded and native units appearing in the unit report, which carry higher eCPM (section 3). Volume barely moved.

---

## 2. Why revenue is so low: root causes, ranked

### RC-1 Traffic is tiny. This alone explains most of the problem. [Confirmed]
About 18 ad requests and 4 impressions a day. Even with a **100 % match rate and a 100 % show rate**, the 446 requests at the observed $1.33 eCPM would have earned about **$0.59** in four weeks. No change to placement or frequency can turn this traffic into meaningful revenue. Revenue = active users × impressions per user × eCPM, and the first term is close to zero.

Rough session estimate **[Hypothesis]**: the main banner and the rewarded unit both made exactly 49 requests in the unit week, and both request once per Home visit or cold start (see RC-4). That suggests roughly **7 app sessions a day across all users**. The DAU / AU columns are empty in both files, so check this against Play Console → Statistics → DAU.

**Implication:** the strategy has to do two things at once: grow installs (ASO, retention) and earn more per user. The ad work below improves the second. It cannot replace the first.

### RC-2 Clicks look like invalid traffic: an account-safety risk. [Confirmed data, Hypothesis cause]
- Overall CTR is **8.3 %**. Typical CTR is roughly 0.5–2 % for banners and somewhat higher for native.
- On 21 Oct, **5 of 10 impressions were clicked (50 %)**. `VixPlay_Banner_VideoList` shows 1 click on 2 impressions (50 %).
- Release builds use **real** ad unit IDs (`AdUnits.kt`), and the app does not register any test device IDs (`AdsController.initializeSdk` sets no `setTestDeviceIds`). Every release build installed on the team's own phones, including Play internal-testing installs, therefore serves live ads.
- **Likely cause:** the team clicking live ads on its own devices. That is the classic trigger for AdMob "ad serving limited" and, if it repeats, account suspension.
- **Possibly related [Hypothesis]:** several days with high request counts and very low match rates came after the 21 Oct click spike: 27 Oct (44 requests, 18 % matched), 31 Oct (81, 11 %), 15 Nov (40, 10 %), 17 Nov (14, 14 %). Limited ad serving shows up as exactly this kind of match-rate collapse. **Check AdMob → Policy center / the account home page for an "Ad serving limit" notice.**

### RC-3 Loaded but not shown: 55 % of matched ads were wasted. [Confirmed]
Of the matched requests, 133 never became impressions. By unit (11–17 Nov):

| Unit | Matched | Shown | Show rate | Cause found in code |
|---|---|---|---|---|
| Rewarded – Pro | 25 | 7 | **28 %** | `PremiumViewModel.init` calls `rewardedAds.preload()`. That view model is created by **`DashboardScreen` (the launch tab)**, so a rewarded ad loads on almost every app open whether or not the user ever taps "Watch ad". Rewarded ads expire after 1 hour, and the expired ones are thrown away. |
| Native – FolderList | 16 | 9 | 56 % | `AdNativeCard` loads inside a `DisposableEffect` of a **LazyColumn item**. Scrolling the item off-screen disposes it, and scrolling back fires a **new request**. Each Video-tab visit also starts a new load. |
| Banner – Main | 37 | 25 | 68 % | `AdBanner` creates a new `AdView` whenever it enters composition (each switch between a tab that shows the banner and the Video tab, which hides it) and **never calls `destroy()`, `pause()` or `resume()`**. |
| Banner – VideoList | 2 | 2 | 100 % | Too few matched requests to judge show rate. The problem is match rate (RC-5). |

### RC-4 Only passive, low-eCPM formats; no full-screen inventory. [Confirmed]
The only formats live are banner, native and user-initiated rewarded. There is **no interstitial** and **no app-open ad**: the `APP_OPEN` unit ID exists in `AdUnits.kt` but nothing renders it. Rewarded already earns **$5.08 eCPM, 2.6× the main banner**, but it can only be seen when a user chooses to watch it. The free tier has no full-screen ad at natural breaks.

### RC-5 `VixPlay_Banner_VideoList` barely fills. [Confirmed]
26 requests, **7.7 % match**, $0.60 eCPM. It renders as the first item of a lazy grid inside the folder video list, so it gets the same dispose-and-reload churn as the native card. A match rate this low on one unit, while the main banner matches 75.5 %, points to the unit itself: a newly created unit still warming up, a size mismatch, or a unit-level setting. **[Hypothesis]**

### RC-6 `VixPlay_Native_VideoList` produced no requests in the unit week. [Confirmed]
It is absent from the unit report. In code it only renders after every 8th video and never after the last one (`VIDEO_LIST_AD_INTERVAL = 8`), so a folder needs **at least 9 videos** before it ever loads. Most folders apparently have fewer.

### RC-7 Rewarded load latency is about 10 s. [Confirmed]
The rewarded unit averages **10.17 s** ad-load latency, and the 16 Nov daily latency is 9.55 s. If no ad is pre-warmed, `RewardedAdManager.show()` loads on demand, and the user taps "Watch ad" and waits up to ten seconds with **no loading indicator**. Some of those users give up, which feeds the low show rate.

---

## 3. Format and ad-unit performance (11–17 Nov) [Confirmed]

| Ad unit | Format | Requests | Match | Show | Impr. | eCPM | Est. $ | Share of revenue | Clicks |
|---|---|---|---|---|---|---|---|---|---|
| VixPlay_Banner_Main | Banner | 49 | 75.5 % | 67.6 % | 25 | $1.97 | 0.049 | **49 %** | 1 |
| VixPlay Rewarded – Pro | Rewarded | 49 | 51.0 % | 28.0 % | 7 | **$5.08** | 0.036 | 35 % | 0 |
| VixPlay_Native_FolderList | Native advanced | 28 | 57.1 % | 56.3 % | 9 | $1.59 | 0.014 | 14 % | 0 |
| VixPlay_Banner_VideoList | Banner | 26 | **7.7 %** | 100 % | 2 | $0.60 | 0.001 | 1 % | 1 |
| VixPlay_Native_VideoList | Native advanced | 0 | — | — | 0 | — | 0 | 0 % | — |

- **Highest eCPM:** rewarded ($5.08).
- **Most revenue:** the main banner, because it has the most impressions.
- **Underperforming:** Banner_VideoList (fill), Native_VideoList (never requested), and rewarded (requests wasted, even though its eCPM is good).
- **Bidding:** bid requests equal requests and win rate is 100 % on every row. **[Hypothesis]** Only the AdMob Network is competing, so there is no mediation or third-party bidding.

---

## 4. Answers to the ten questions

| # | Question | Answer |
|---|---|---|
| 1 | Why is revenue low? | Mostly RC-1 (very few users). Then RC-3 (55 % of matched ads never shown), RC-4 (no full-screen formats), RC-2 (possible serving limits from invalid clicks). |
| 2 | Enough ad requests? | No. About 18 a day across the whole app. |
| 3 | Is the match rate OK? | 54 % overall is poor. It is 100 % on most days but collapses on high-request days (RC-2), and Banner_VideoList is 7.7 %. |
| 4 | Requested but not displayed? | Yes, for 55 % of matched requests (RC-3). |
| 5 | Best format? | Rewarded by eCPM, the main banner by total revenue. |
| 6 | Best and worst countries by eCPM? | **Cannot answer.** Neither file has a country dimension. Re-export with *Country* as a dimension. |
| 7 | Underperforming units? | Banner_VideoList, Native_VideoList, and rewarded (waste, not eCPM). |
| 8 | Unusual low days? | Zero-impression days: 22 Oct, 28 Oct, 6 Nov, 10 Nov, 17 Nov. Match-rate collapses: 27 Oct, 31 Oct, 15 Nov, 17 Nov. No traffic at all: 29 Oct, 8 Nov, 14 Nov. |
| 9 | Losing revenue to placement, frequency or implementation? | Implementation yes (RC-3, RC-5, RC-6, RC-7). Frequency yes (RC-4, no interstitial). Placement: banner placement is reasonable. |
| 10 | Biggest opportunities? | (a) Protect the account (test devices, no self-clicks). (b) Stop wasting matched requests. (c) Add a capped interstitial at natural breaks. (d) Make rewarded instant and honestly worth watching. (e) Grow DAU. |

---

## 5. Metrics missing from the export (not estimated)

These are absent or empty in the files and must not be guessed:

- **Active users, DAU, ad viewers, ARPU, ARPDAU, impressions per user**: the columns exist but are empty in both files.
- **Country**, **platform/OS version**, **device**: no such dimension.
- **Ad source / mediation breakdown**: only the AdMob Network appears.
- **Monthly trend**: four weeks of data is not enough.
- **Show rate by placement inside a screen**: requires in-app analytics (see the technical design).

**To get them next time:** in AdMob → Reports, add the dimensions *Country*, *Ad unit × Date*, *App version* and *Ad source*. Link AdMob to the Firebase project `devbytes-d260c` so the user metrics fill in.

---

## 6. Improvement opportunities, ranked by expected impact and confidence

1. **Account safety (do first):** register test device IDs, stop clicking live ads, and check the Policy center. Losing the account means zero revenue.
2. **Recover wasted matched requests:** rewarded preload only when it is likely to be used; native ads cached across scroll; banner lifecycle handled properly.
3. **Add a capped interstitial at natural breaks** (leaving the player after real watch time). This is the largest per-user lever not yet used. Its eCPM for this app is **unknown until measured**, so no figure is promised.
4. **Fix the two weak units**: investigate Banner_VideoList fill, and lower the native interval so Native_VideoList actually renders.
5. **Turn on per-impression revenue tracking** (`OnPaidEventListener` → Firebase `ad_impression`) so ARPDAU can be measured instead of inferred.
6. **Grow DAU.** Outside ad engineering, but it multiplies everything above.
