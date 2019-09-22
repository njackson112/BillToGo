package com.example.billtogo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.inputmethodservice.InputMethodService;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.bill.java.api.BDC;
import com.bill.java.api.exception.BDCException;
import com.bill.java.api.models.Bill;
import com.bill.java.api.models.Customer;
import com.bill.java.api.models.Invoice;
import com.bill.java.api.models.MFA;
import com.bill.java.api.models.MFAChallenge;
import com.bill.java.api.models.OrgInfo;
import com.bill.java.api.models.ReceivedPay;
import com.bill.java.api.models.Session;
import com.bill.java.api.models.SessionInfo;
import com.bill.java.api.models.Vendor;
import com.bill.java.api.param.BillCreateRequestParams;
import com.bill.java.api.param.CustomerCreateRequestParams;
import com.bill.java.api.param.InvoiceCreateRequestParams;
import com.bill.java.api.param.MFAAuthenticateRequestParams;
import com.bill.java.api.param.MFAChallengeRequestParams;
import com.bill.java.api.param.ReceivedPayGetRequestParams;
import com.bill.java.api.param.RecordARPaymentRequestParams;
import com.bill.java.api.param.SessionLoginRequestParams;
import com.bill.java.api.param.VendorCreateRequestParams;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;

import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends WearableActivity
    implements View.OnClickListener {

    private TextView mTextView;
    private EditText mEditText;
    private Button mButton;
    private Button mButton2;
    private RecyclerView mItems;
    private String mfaToken = "";
    private MFAChallenge mfaChallenge;
    private String[] mfaID;
    private HashMap<String, Customer> customer_list = new HashMap<>();
    private HashMap<String, Vendor> vendor_list = new HashMap<>();
    private ArrayList<String> customer_names = new ArrayList<>();
    private ArrayList<String> vendor_names = new ArrayList<>();
    private Customer activeCustomer;
    private String activeCustomername = "";
    private String activeVendorName = "";
    private HashMap<String, ArrayList<Invoice>> invoices = new HashMap<>();
    private HashMap<String, ArrayList<Bill>> bills = new HashMap<>();
    private HashMap<String, ArrayList<Invoice>> ars = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.starting_page);

        //mEditText = findViewById(R.id.enter_code);
        mButton = findViewById(R.id.get_started);
        mButton.setOnClickListener(this);

        // Enables Always-on
        setAmbientEnabled();

        BDC.userName = "nathanljackson@att.net";
        BDC.password = "Njaxson10!";
        BDC.devKey = "01JFJIGOPULHADWJD201";
        BDC.setApiBase(BDC.Env.SANDBOX);

        final OrgInfo[] orgID = new OrgInfo[1];

        Thread login = new Thread(() -> {
            try {
                orgID[0] = Session.ListOrgs().get(0);
                SessionLoginRequestParams params = SessionLoginRequestParams.builder()
                        .with($ -> {
                            $.orgId = orgID[0].getOrgId();
                        }).build();
                Session.login(params);
            } catch (BDCException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mfaID = new String[1];

            MFAChallengeRequestParams challengeRequestParams = new MFAChallengeRequestParams.Builder()
                    .with($ -> {
                        $.useBackup = false;
                    }).build();

            try {
                mfaChallenge = Session.requestMFAChallenge(challengeRequestParams);
            } catch (BDCException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        });
        login.start();
    }

    private void doMFA(){
        Thread mfa = new Thread(() -> {
        //remember me tick?
        boolean rememberMe = true;

        //run auth. Device ID and Machine Name arbitrary, token needs to be set up
        MFAChallenge finalMfaChallenge = mfaChallenge;
        MFAAuthenticateRequestParams params = MFAAuthenticateRequestParams.builder()
            .with($ -> {
                $.challengeId = finalMfaChallenge.getId();
                $.token = mfaToken;
                $.deviceId = "78910";
                $.machineName = "Nathan's Sports Watch";
                $.rememberMe = true;
            }).build();

        //will return mfaID
        try {
            MFA mfaObj = Session.MFAAuthenticate(params);
            mfaID[0] = mfaObj.getMfaId();
        } catch (BDCException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(mfaToken);
        });
        mfa.start();
    }

    private void receivables(){
        Thread receivables = new Thread(() -> {

        });
        receivables.start();
    }

    private void populateCustomers(String cust_name, String acct, String company){
        Thread newCust = new Thread(() -> {
        CustomerCreateRequestParams customerParams = CustomerCreateRequestParams.builder()
                .with($ -> {
                    $.name = cust_name;
                    $.accNumber = acct;
                    $.companyName = company;
                }).build();
            Customer newCustomer = null;
        try {
            newCustomer = Customer.create(customerParams);
        } catch (BDCException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String n = newCustomer.getName();
        activeCustomername = n;
        System.out.println(newCustomer.getName());
        System.out.println(cust_name);
        customer_list.put(n, newCustomer);
        customer_names.add(n);
        });
        newCust.start();
    }

    private void populateVendors(String name, String acct, String company){
        Thread newVend = new Thread(() -> {
            VendorCreateRequestParams vendorParams = VendorCreateRequestParams.builder()
                    .with($ -> {
                        $.name = name;
                        $.accNumber = acct;
                        $.companyName = company;
                    }).build();
            Vendor newVendor = null;
            try {
                newVendor = Vendor.create(vendorParams);
            } catch (BDCException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            vendor_list.put(activeVendorName, newVendor);
            vendor_names.add(activeVendorName);
        });
        newVend.start();
    }

    private void populateInvoices(Customer cust, String invoice_number, String invoice_Date, String due_Date){
        Thread invoice = new Thread(() -> {
            InvoiceCreateRequestParams invoiceParams = InvoiceCreateRequestParams.builder()
                    .with($ -> {
                        $.customerId = cust.getId();
                        $.invoiceNumber = invoice_number;
                        $.invoiceDate = invoice_Date;
                        $.dueDate = due_Date;
                    }).build();
            Invoice newInvoice= null;
            try {
                newInvoice = Invoice.create(invoiceParams);
            } catch (BDCException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(invoices.containsKey(cust.getName())){
                ArrayList<Invoice> invoice_list = invoices.get(cust.getName());
                invoice_list.add(newInvoice);
                invoices.put(cust.getName(), invoice_list);
            } else {
                ArrayList<Invoice> invoice_list = new ArrayList<>();
                invoice_list.add(newInvoice);
                invoices.put(cust.getName(), invoice_list);
            }
        });
        invoice.start();
    }

    private void populateBills(Vendor vend, String bill_number, String bill_Date, String bill_Due_Date){
        Thread bill = new Thread(() -> {
            BillCreateRequestParams billParams = BillCreateRequestParams.builder()
                    .with($ -> {
                        $.vendorId = vend.getId();
                        $.invoiceNumber = bill_number;
                        $.invoiceDate = bill_Date;
                        $.dueDate = bill_Due_Date;
                        //$.amount =

                    }).build();
            Bill newBill = null;
            try {
                newBill = Bill.create(billParams);
            } catch (BDCException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(invoices.containsKey(vend.getName())){
                ArrayList<Bill> bill_list = bills.get(vend.getName());
                bill_list.add(newBill);
                bills.put(vend.getName(), bill_list);
            } else {
                ArrayList<Bill> bill_list = new ArrayList<>();
                bill_list.add(newBill);
                bills.put(vend.getName(), bill_list);
            }
        });
        bill.start();
    }

//    private void populateAR(Customer cust, String payment_Date, String payment_Type, BigDecimal amountPaid){
//        Thread invoice = new Thread(() -> {
//            RecordARPaymentRequestParams arParams = RecordARPaymentRequestParams.builder()
//                    .with($ -> {
//                        $.customerId = cust.getId();
//                        $.paymentDate = payment_Date;
//                        $.paymentType = payment_Type;
//                        $.amount = amountPaid;
//                    }).build();
//            ReceivedPay arPayment ;
//            try {
//                newInvoice = Invoice.create(invoiceParams);
//            } catch (BDCException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            if(invoices.containsKey(cust.getName())){
//                ArrayList<Invoice> invoice_list = invoices.get(cust.getName());
//                invoice_list.add(newInvoice);
//                invoices.put(cust.getName(), invoice_list);
//            } else {
//                ArrayList<Invoice> invoice_list = new ArrayList<>();
//                invoice_list.add(newInvoice);
//                invoices.put(cust.getName(), invoice_list);
//            }
//        });
//        invoice.start();
//    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.get_started:
                setContentView(R.layout.activity_main);
                mEditText = findViewById(R.id.enter_code);
                mButton = findViewById(R.id.button_id);
                mButton.setOnClickListener(this);
                break;

            case R.id.button_id:
                EditText content = (EditText)findViewById(R.id.enter_code);
                String token = content.getText().toString();
                mfaToken = token;
                System.out.println(mfaToken);
                doMFA();
                setContentView(R.layout.this_or_that);
                mButton = findViewById(R.id.receivables);
                mButton.setOnClickListener(this);
                mButton2 = findViewById(R.id.payments);
                mButton2.setOnClickListener(this);
                break;
            case R.id.receivables:
                customer_list = new HashMap<>();
                populateCustomers("Cust Omer", "777", "Cust Co.");
                populateCustomers("Miss Nomer", "778", "Missing No.");
                populateCustomers("Nathan Jackson", "779", "Nathan's Hotdogs");
                setContentView(R.layout.receivables_customer_select);
                mButton = findViewById(R.id.new_customer);
                mButton.setOnClickListener(this);
                mButton2 = findViewById(R.id.nathan_jackson);
                mButton2.setOnClickListener(this);
                break;

            case R.id.new_customer:
                //switch view to add customer info
                setContentView(R.layout.add_customer);
                mButton = findViewById(R.id.submit_customer);
                mButton.setOnClickListener(this);
                break;

            case R.id.nathan_jackson:
                activeCustomername = "Nathan Jackson";
                setContentView(R.layout.create_invoice);
                mButton = findViewById(R.id.submit_invoice);
                mButton.setOnClickListener(this);
                break;

            case R.id.submit_customer:
                //we will need customerID for invoice, so pass along name
                EditText name = (EditText) findViewById(R.id.enter_name);
                EditText acct = findViewById(R.id.enter_acct);
                EditText company = findViewById(R.id.enter_company);
                String name_text = name.getText().toString();
                String acct_text = acct.getText().toString();
                String company_text = company.getText().toString();
                populateCustomers(name_text, acct_text, company_text);
                activeCustomer = customer_list.get(activeCustomername);
                setContentView(R.layout.create_invoice);
                mButton = findViewById(R.id.submit_invoice);
                mButton.setOnClickListener(this);
                break;

            case R.id.submit_invoice:
                EditText invoice_number = findViewById(R.id.invoice_number);
                EditText invoice_date = findViewById(R.id.invoice_date);
                EditText due_date = findViewById(R.id.due_Date);
                String invoice_num = invoice_number.getText().toString();
                String invoiceDate = invoice_date.getText().toString();
                String dueDate = due_date.getText().toString();
                populateInvoices(customer_list.get(activeCustomername), invoice_num, invoiceDate, dueDate);
                setContentView(R.layout.record_ar);
                mButton = findViewById(R.id.submit_ar);
                mButton.setOnClickListener(this);
                break;

            case R.id.submit_ar:
                EditText payment_date = findViewById(R.id.payment_date);
                EditText payment_Type = findViewById(R.id.payment_type);
                EditText amount = findViewById(R.id.amount);
                //populateAR(activeCustomer, payment_date.toString(), payment_Type.toString(), new BigDecimal(amount.toString()));
                setContentView(R.layout.end_receivable);
                mButton = findViewById(R.id.main_menu_ar);
                mButton.setOnClickListener(this);
                break;

            case R.id.main_menu_ar:
                setContentView(R.layout.this_or_that);
                mButton = findViewById(R.id.receivables);
                mButton.setOnClickListener(this);
                mButton2 = findViewById(R.id.payments);
                mButton2.setOnClickListener(this);
                break;

            case R.id.payments:
                vendor_list = new HashMap<>();
                populateVendors("Wait Lister", "779", "Waitlisters Inc.");
                setContentView(R.layout.payables_vendors_select);
                mButton = findViewById(R.id.new_vendor);
                mButton.setOnClickListener(this);
                mButton2 = findViewById(R.id.wait_lister);
                mButton2.setOnClickListener(this);
                break;

            case R.id.new_vendor:
                setContentView(R.layout.add_vendor);
                mButton = findViewById(R.id.submit_vendor);
                mButton.setOnClickListener(this);
                break;

            case R.id.submit_vendor:
                EditText vendor_name = (EditText) findViewById(R.id.enter_name);
                EditText vendor_acct = findViewById(R.id.enter_acct);
                EditText vendor_company = findViewById(R.id.enter_company);
                String vendor_name_text = vendor_name.getText().toString();
                String vendor_acct_text = vendor_acct.getText().toString();
                String vendor_company_text = vendor_company.getText().toString();
                populateVendors(vendor_name_text, vendor_acct_text, vendor_company_text);
                activeVendorName = vendor_name_text;
                setContentView(R.layout.create_bill);
                mButton = findViewById(R.id.submit_bill);
                mButton.setOnClickListener(this);
                break;

            case R.id.submit_bill:
                EditText bill_number = findViewById(R.id.bill_number);
                EditText bill_date = findViewById(R.id.bill_date);
                EditText bill_due_date = findViewById(R.id.due_Date);
                //EditText bill_amount = findViewById(R.id.bill_amount);
                String bill_num = bill_number.getText().toString();
                String billDate = bill_date.getText().toString();
                String billDueDate = bill_due_date.getText().toString();
                //String billAmount = bill_amount.getText().toString();
                //populateBills(vendor_list.get(activeVendorName), bill_num, billDate, billDueDate, billAmount);
                populateBills(vendor_list.get(activeVendorName), bill_num, billDate, billDueDate);
                setContentView(R.layout.record_ap);
                mButton = findViewById(R.id.submit_ap);
                mButton.setOnClickListener(this);
                break;

            case R.id.submit_ap:
                EditText process_date = findViewById(R.id.process_date);
                EditText print_check = findViewById(R.id.print_check);
                EditText ap_amount = findViewById(R.id.amount);
                //populateAP(activeCustomer, payment_date.toString(), payment_Type.toString(), new BigDecimal(amount.toString()));
                setContentView(R.layout.end_payable);
                mButton = findViewById(R.id.main_menu_ap);
                mButton.setOnClickListener(this);
                break;

            case R.id.main_menu_ap:
                setContentView(R.layout.this_or_that);
                mButton = findViewById(R.id.receivables);
                mButton.setOnClickListener(this);
                mButton2 = findViewById(R.id.payments);
                mButton2.setOnClickListener(this);
                break;

            case R.id.wait_lister:
                activeCustomername = "Wait Lister";
                setContentView(R.layout.create_bill);
                mButton = findViewById(R.id.submit_bill);
                mButton.setOnClickListener(this);
                break;

            default:
                System.out.println("not finding button");
                setContentView(R.layout.this_or_that);
                mButton = findViewById(R.id.receivables);
                mButton.setOnClickListener(this);
                mButton2 = findViewById(R.id.payments);
                mButton2.setOnClickListener(this);
                break;
        }

    }
}