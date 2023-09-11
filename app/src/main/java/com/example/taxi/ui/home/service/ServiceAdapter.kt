package com.example.taxi.ui.home.service

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.taxi.R
import com.example.taxi.domain.model.tarif.Mode
import com.example.taxi.ui.home.tarif.ModeToggleInterface
import com.example.taxi.utils.PhoneNumberUtil
import com.example.taxi.utils.convertToCyrillic
import kotlinx.coroutines.launch

class ServiceAdapter(
    private val list: List<Mode>,
    private val modeToggleInterface: ModeToggleInterface,
    private val lifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder>() {

    inner class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleName: TextView = itemView.findViewById(R.id.service_title_txt)
        val servicePrice: TextView = itemView.findViewById(R.id.service_price)

        //        val price: TextView = itemView.findViewById(R.id.textView2p)
        val switchEnabled: SwitchCompat = itemView.findViewById(R.id.service_switch)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_service, parent, false)
        return ServiceViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        with(holder) {
            titleName.convertToCyrillic(list[position].name)
            servicePrice.text = PhoneNumberUtil.formatMoneyNumberPlate(list[position].cost)
            switchEnabled.isChecked = list[position].value == "1"

            switchEnabled.setOnCheckedChangeListener { _, _ ->
                setSwitch(
                    position = position,
                    switchEnabled = switchEnabled,
                    context = holder.itemView.context,
                    modeToggleInterface = modeToggleInterface
                )
            }

            itemView.setOnClickListener {
                switchEnabled.isChecked = !switchEnabled.isChecked
            }
        }
    }

    private fun setSwitch(
        position: Int,
        switchEnabled: SwitchCompat,
        context: Context,
        modeToggleInterface: ModeToggleInterface
    ) {

        val tarifStatus = if (switchEnabled.isChecked) {
            context.getString(R.string.tarif_yoqildi)
        } else {
            context.getString(R.string.tarif_nofaol)
        }

        val message = "${list[position].name} $tarifStatus"
        val title = context.getString(R.string.tarif_rejim)

        lifecycleOwner.lifecycleScope.launch {
            modeToggleInterface.toggle(
                id = list[position].id,
                title = title,
                message = message,
                color = switchEnabled.isChecked
            )
        }
    }
}