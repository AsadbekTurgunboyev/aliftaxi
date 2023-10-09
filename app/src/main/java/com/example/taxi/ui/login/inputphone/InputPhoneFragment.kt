package com.example.taxi.ui.login.inputphone

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.taxi.R
import com.example.taxi.databinding.FragmentInputPhoneBinding
import com.example.taxi.domain.model.MainResponse
import com.example.taxi.domain.model.register.RegisterData
import com.example.taxi.domain.model.register.RegisterRequest
import com.example.taxi.domain.preference.UserPreferenceManager
import com.example.taxi.utils.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class InputPhoneFragment : Fragment() {

    private lateinit var viewBinding: FragmentInputPhoneBinding
    private val registerViewModel: RegisterViewModel by viewModel()
    private val preferenceManager: UserPreferenceManager by inject()
    private lateinit var loadingDialog: Dialog
    private lateinit var blockDialog: Dialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        viewBinding = FragmentInputPhoneBinding.inflate(layoutInflater, container, false)
        loadingDialog = DialogUtils.loadingDialog(requireContext())
        blockDialog = DialogUtils.blockDialog(requireContext())
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        createPhoneNumberPlateEditText(viewBinding.edtInputPhone, viewBinding.textInputLayout)
        registerViewModel.registerResponse.observe(viewLifecycleOwner, ::updateView)

        viewBinding.fbnInputPhone.setOnClickListener {
            if (!validateInputs()) return@setOnClickListener

            val phone = "${viewBinding.textInputLayout.prefixText} ${viewBinding.edtInputPhone.text}"

            registerViewModel.register(
                RegisterRequest(
                    PhoneNumberUtil.formatPhoneNumber(phoneNumber = phone, countryCode = "UZ")
                        .toString()
                )
            )
        }

    }

    private fun validateInputs(): Boolean {
        with(viewBinding) {
            if (!isValidInput(
                    textInputLayout,
                    ValidationUtils::isValidPhoneNumber,
                    getString(R.string.input_phone_number)
                )
            ) return false
        }
        return true
    }

    private fun isValidInput(
        input: TextInputLayout,
        validationFunction: (String) -> Boolean,
        errorMessage: String
    ): Boolean {
        if (!validationFunction(input.editText?.text.toString())) {
            input.error = errorMessage
            return false
        }
        input.error = null
        return true
    }

    private fun updateView(resource: Resource<MainResponse<RegisterData>>?) {
        resource?.let {
            when (resource.state) {
                ResourceState.LOADING -> {
                    loadingDialog.show()
                }
                ResourceState.SUCCESS -> {
                    loadingDialog.dismiss()
                    if (it.data?.status == 203){
                        blockDialogShow(it.data.data)

                    }else{
                        processSuccessState(resource.data)
                    }
                }
                ResourceState.ERROR -> {
                    loadingDialog.dismiss()
                    Toast.makeText(requireContext(), "${resource.message} ${resource.data}", Toast.LENGTH_SHORT).show()

                }
            }
        }

    }

    private fun blockDialogShow(data: RegisterData) {
        val error : TextView = blockDialog.findViewById(R.id.txt_error_desc_error_block)
        val callButton : MaterialButton = blockDialog.findViewById(R.id.btn_call_to_dispetcher)
        val cancelButton: MaterialButton = blockDialog.findViewById(R.id.btn_call_to_cancel)
        error.text = data.message
        blockDialog.show()

        callButton.setOnClickListener {
            ButtonUtils.callToDispatchWhenBlock(requireContext(),data.phone)
        }

        cancelButton.setOnClickListener {

            blockDialog.dismiss()
        }

    }

    private fun processSuccessState(response: MainResponse<RegisterData>?) {
        response?.data?.let {
            preferenceManager.apply {
                saveToken(it.token)
                savePhone(it.phone)
            }
            registerViewModel.clear()
            viewBinding.edtInputPhone.text?.clear()
            findNavController().navigate(R.id.inputPasswordFragment)
        }
    }


}