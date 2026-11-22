package com.devbytes.vixplayer.app.ads

import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.ResponseInfo

/** Logs an AdMob paid event (`OnPaidEventListener`) as Firebase's `ad_impression`. */
fun AdEvents.paid(format: AdFormat, unitName: String, value: AdValue, info: ResponseInfo?) =
    paid(format, unitName, value.valueMicros, value.currencyCode, info?.loadedAdapterResponseInfo?.adSourceName)
