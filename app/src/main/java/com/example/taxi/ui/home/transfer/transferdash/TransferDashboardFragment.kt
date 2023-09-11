package com.example.taxi.ui.home.transfer.transferdash

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.example.taxi.R
import com.example.taxi.databinding.FragmentTransferDashboardBinding
import com.example.taxi.domain.model.IsCompletedModel
import com.example.taxi.domain.model.MainResponse
import com.example.taxi.domain.model.balance.BalanceData
import com.example.taxi.domain.model.selfie.SelfieAllData
import com.example.taxi.domain.model.selfie.StatusModel
import com.example.taxi.ui.home.dashboard.DashboardViewModel
import com.example.taxi.utils.PhoneNumberUtil
import com.example.taxi.utils.Resource
import com.example.taxi.utils.ResourceState
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class TransferDashboardFragment : Fragment() {

    lateinit var viewBinding: FragmentTransferDashboardBinding
    val dashboardViewModel : DashboardViewModel by sharedViewModel()

    val navController : NavController by lazy {
        findNavController()
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        viewBinding = FragmentTransferDashboardBinding.inflate(layoutInflater,container,false)
        // Inflate the layout for this fragment
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.transferMoneyButton.setOnClickListener {
            navController.navigate(R.id.transferMoneyFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            dashboardViewModel.getBalance()
            dashboardViewModel.getDriverData()
        }

        dashboardViewModel.balanceResponse.observe(viewLifecycleOwner){
            updateMoneyUi(it)
        }

        dashboardViewModel.driverDataResponse.observe(viewLifecycleOwner){
            setDataUi(it)
        }

        viewBinding.fbnBackHome.setOnClickListener {
            navController.navigateUp()
        }

        viewBinding.transaction.setOnClickListener {
            navController.navigate(R.id.transferHistoryFragment)
        }
    }

    private fun setDataUi(resource: Resource<MainResponse<SelfieAllData<IsCompletedModel,StatusModel>>>?) {
        resource?.let {
            when(it.state){
                ResourceState.ERROR ->{}
                ResourceState.SUCCESS ->{
                    viewBinding.driverId.text = it.data!!.data.id.toString()
                }
                ResourceState.LOADING ->{}
            }
        }
    }

    private fun updateMoneyUi(resource: Resource<MainResponse<BalanceData>>?) {
        resource?.let {
            when(it.state){
                ResourceState.LOADING ->{}
                ResourceState.SUCCESS ->{
                    viewBinding.balanceValue.text = PhoneNumberUtil.formatMoneyNumberPlate(resource.data!!.data.total.toString())
                }
                ResourceState.ERROR ->{}
            }
        }
    }
}