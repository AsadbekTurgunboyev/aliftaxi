package com.example.taxi.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.example.taxi.R
import com.example.taxi.domain.preference.UserPreferenceManager

object MenuUtils {
    fun getActionViewLanguage(
        context: Context,
        preferenceManager: UserPreferenceManager,

        ): View{
        val actionView = LayoutInflater.from(context).inflate(R.layout.item_menu_language, null)

        val language: TextView = actionView.findViewById(R.id.which_language)
        language.text = when(preferenceManager.getLanguage()){
            UserPreferenceManager.Language.RUSSIAN -> "Русский язык"
            UserPreferenceManager.Language.UZBEK -> "O'zbek tili"
            UserPreferenceManager.Language.KRILL -> "Узбек тили"
        }


        return actionView

    }

    fun getActionViewNext(
        context: Context,
    ): View {

        return LayoutInflater.from(context).inflate(R.layout.item_action_next, null)

    }
    private fun getActionViewForItem3(context: Context): View {
        val actionView = LayoutInflater.from(context).inflate(R.layout.item_menu_language, null)

//        val actionViewButton = actionView.findViewById<Button>(R.id.actionViewButton)
//        actionViewButton.text = "Action for Item 3"

        // Set any necessary listeners or modifications to the action view for Item 3

        return actionView
    }



}


class MenuClass(val context: Context, val preferenceManager: UserPreferenceManager) {
    private val groups: List<Triple<Int, String, List<Triple<Int, String, Int>>>> = listOf(
        Triple(
            123, "Group 1", listOf(
                Triple(456, context.getString(R.string.til), R.drawable.ic_lang)
            )
        ),
        Triple(
            234, "Group 2", listOf(
                Triple(987, context.getString(R.string.about_us), R.drawable.ic_info)
            )
        ),
        Triple(
            235, "Group 3", listOf(
                Triple(988, context.getString(R.string.faq), R.drawable.ic_faq)
            )
        ),
        Triple(
            236, "Group 4", listOf(
                Triple(989, context.getString(R.string.baho_bering), R.drawable.ic_star)
            )
        ),
        Triple(
            237,"Group 5", listOf(
                Triple(990,context.getString(R.string.sign_out),R.drawable.baseline_logout_24)
            )
        )

    )

    private val itemActionViews: Map<Int, View> = mapOf(
        456 to MenuUtils.getActionViewLanguage(context,preferenceManager),
        987 to MenuUtils.getActionViewNext(context),
        988 to MenuUtils.getActionViewNext(context),
        989 to MenuUtils.getActionViewNext(context)
    )

    fun getMenu(): List<Triple<Int, String, List<Triple<Int, String, Int>>>> {
        return groups
    }

    fun getItemActionView(): Map<Int, View> {
        return itemActionViews
    }




    // Implement other necessary functions or methods here
}

