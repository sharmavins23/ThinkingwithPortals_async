package com.xora.thinkingwithportals_async

import android.content.Context
import android.os.AsyncTask
import android.widget.Toast
import org.json.JSONObject

class NetworkAsyncCall(
    private val context: Context,
    private val url: String,
    private val requestType: String,
    private val postJSONObject: JSONObject = JSONObject()
) : AsyncTask<String?, String?, String?>() {

    override fun doInBackground(vararg p0: String?): String? {
        return when (requestType) {
            RequestHandler.GET -> RequestHandler.requestGET(url)
            RequestHandler.GET -> RequestHandler.requestPOST(url, postJSONObject)
            else -> ""
        }
    }

    override fun onPostExecute(s: String?) {
        if (s != null) {
            Toast.makeText(context, "Assets loaded.", Toast.LENGTH_LONG).show()
        }
    }
}