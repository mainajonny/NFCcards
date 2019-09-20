package com.example.meech.formatcards;

import android.annotation.TargetApi;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.example.meech.formatcards.CardUtility.KeyInfoProvider;
import com.example.meech.formatcards.CardUtility.SampleAppKeys;
import com.nxp.nfclib.CardType;
import com.nxp.nfclib.KeyType;
import com.nxp.nfclib.NxpNfcLib;
import com.nxp.nfclib.desfire.DESFireFactory;
import com.nxp.nfclib.desfire.IDESFireEV1;
import com.nxp.nfclib.exceptions.NxpNfcLibException;
import com.nxp.nfclib.interfaces.IKeyData;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class Format_card extends AppCompatActivity {

    private IKeyData objKEY_2KTDES_ULC = null;
    NfcAdapter nfcAdapter;
    private IKeyData objKEY_2KTDES = null;
    private IKeyData objKEY_AES128 = null;
    private byte[] default_ff_key = null;
    private IKeyData default_zeroes_key = null;
    private NxpNfcLib libInstance = null;
    private IDESFireEV1 desFireEV1;
    private CardType mCardType = CardType.UnknownCard;
    private Cipher cipher = null;
    private byte[] bytesKey = null;
    private IvParameterSpec iv = null;
    private static final String KEY_APP_MASTER = "This is my key  ";
    private static final String ALIAS_KEY_AES128 = "key_aes_128";
    private static final String ALIAS_KEY_2KTDES = "key_2ktdes";
    private static final String ALIAS_KEY_2KTDES_ULC = "key_2ktdes_ulc";
    private static final String ALIAS_DEFAULT_FF = "alias_default_ff";
    private static final String ALIAS_KEY_AES128_ZEROES = "alias_default_00";
    private static final String EXTRA_KEYS_STORED_FLAG = "keys_stored_flag";
    static String packageKey = "b927aab8842ab54cbaf2ea1df9917159";
    byte[] appId = new byte[]{0x1, 0x00, 0x00};

    MaterialDialog pDialogg, pDialog;
    int timeOut = 2000;
    boolean aflag = false;

    Button format;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_format_card);

        format = findViewById(R.id.Format);
        format.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                aflag=true;
                pDialogg = new MaterialDialog.Builder(Format_card.this)
                        .title("TAP CARD TO FORMAT")
                        .content("")
                        .progress(true, 0)
                        .progressIndeterminateStyle(true)
                        .cancelable(false)
                        .show();
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        pDialogg.dismiss();
                        aflag = false;

                    }

                }, 8000);
            }
        });

        initializeLibrary();
        initializeKeys();
        initializeCipherinitVector();

        try{
            nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        }catch (Exception e){

        }
    }


    @TargetApi(19)
    private void initializeLibrary() {
        libInstance = NxpNfcLib.getInstance();
        try {
            libInstance.registerActivity(Format_card.this, packageKey);
        } catch (NxpNfcLibException ex) {
            ex.printStackTrace();
        }
    }

    private void initializeKeys() {
        KeyInfoProvider infoProvider = KeyInfoProvider.getInstance(Format_card.this.getApplicationContext());
        infoProvider.setKey(ALIAS_KEY_2KTDES, SampleAppKeys.EnumKeyType.EnumDESKey, SampleAppKeys.KEY_2KTDES);
        infoProvider.setKey(ALIAS_KEY_AES128, SampleAppKeys.EnumKeyType.EnumAESKey, SampleAppKeys.KEY_AES128);
        infoProvider.setKey(ALIAS_KEY_AES128_ZEROES, SampleAppKeys.EnumKeyType.EnumAESKey, SampleAppKeys.KEY_AES128_ZEROS);
        infoProvider.setKey(ALIAS_DEFAULT_FF, SampleAppKeys.EnumKeyType.EnumMifareKey, SampleAppKeys.KEY_DEFAULT_FF);
        objKEY_2KTDES_ULC = infoProvider.getKey(ALIAS_KEY_2KTDES_ULC, SampleAppKeys.EnumKeyType.EnumDESKey);
        objKEY_2KTDES = infoProvider.getKey(ALIAS_KEY_2KTDES, SampleAppKeys.EnumKeyType.EnumDESKey);
        objKEY_AES128 = infoProvider.getKey(ALIAS_KEY_AES128, SampleAppKeys.EnumKeyType.EnumAESKey);
        default_zeroes_key = infoProvider.getKey(ALIAS_KEY_AES128_ZEROES, SampleAppKeys.EnumKeyType.EnumAESKey);
        default_ff_key = infoProvider.getMifareKey(ALIAS_DEFAULT_FF);
    }


    private void initializeCipherinitVector() {

        /* Initialize the Cipher */
        try {
            cipher = Cipher.getInstance("AES/CBC/NoPadding");
        } catch (NoSuchAlgorithmException e) {

            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }

        /* set Application Master Key */
        bytesKey = KEY_APP_MASTER.getBytes();

        /* Initialize init vector of 16 bytes with 0xCD. It could be anything */
        byte[] ivSpec = new byte[16];
        Arrays.fill(ivSpec, (byte) 0xCD);
        iv = new IvParameterSpec(ivSpec);

    }

    @Override
    protected void onPause() {
        libInstance.stopForeGroundDispatch();
        super.onPause();
    }


    @Override
    protected void onResume() {
        libInstance.startForeGroundDispatch();
        super.onResume();
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (aflag) {
            Log.e("Executing onNewIntent", "######");
            CardType type = CardType.UnknownCard;
            try {
                type = libInstance.getCardType(intent);
            } catch (NxpNfcLibException ex) {

                pDialogg.dismiss();
                new SweetAlertDialog(Format_card.this, SweetAlertDialog.WARNING_TYPE)
                        .setTitleText("ERROR")
                        .setContentText("NFC error")
                        .setConfirmText("OK")
                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sDialog) {
                                sDialog.dismissWithAnimation();

                            }
                        })
                        .show();
            }

            switch (type) {


                case DESFireEV1: {
                    mCardType = CardType.DESFireEV1;
                    desFireEV1 = DESFireFactory.getInstance().getDESFire(libInstance.getCustomModules());
                    try {

                        pDialogg.dismiss();
                        desFireEV1.getReader().connect();
                        desFireEV1.getReader().setTimeout(2000);
                        desfireEV1CardLogic();


                    } catch (Exception t) {
                        t.printStackTrace();
                        Toast.makeText(Format_card.this, "Error", Toast.LENGTH_SHORT).show();


                    }
                    break;
                }


            }
        }
    }


    private void desfireEV1CardLogic() {
        pDialog = new MaterialDialog.Builder(Format_card.this)
                .title("Formatting Card!")
                .content("Please don't remove the card...")
                .progress(true, 0)
                .progressIndeterminateStyle(false)
                .show();
        pDialog.setCancelable(false);
        new FormatCard().execute();


    }

    //Remove Details From Card
    private class FormatCard extends AsyncTask<String, String, String> {


        @Override
        protected String doInBackground(String... params) {
            try {
                Log.e("#######", "Formatting card");


                String uidhex = new String();
                byte[] uid = desFireEV1.getUID();
                for (int i = 0; i < uid.length; i++) {
                    String x = Integer.toHexString(((int) uid[i] & 0xff));
                    if (x.length() == 1) {
                        x = '0' + x;
                    }
                    uidhex += x;
                }



            } catch (Exception ex) {

                Log.i("Exception error", ex.toString());
                return "0";
            }

            return "1";
        }


        protected void onPostExecute(final String result) {
            Log.d("#####RESPONSE", result);

            if (result.equalsIgnoreCase("1")) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {

                            desFireEV1.getReader().setTimeout(timeOut);
                            desFireEV1.selectApplication(0);
                            desFireEV1.authenticate(0, IDESFireEV1.AuthType.Native, KeyType.THREEDES, objKEY_2KTDES);
                            desFireEV1.format();

                            Format_card.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pDialog.dismiss();
                                    aflag = false;
                                    new SweetAlertDialog(Format_card.this, SweetAlertDialog.SUCCESS_TYPE)
                                            .setTitleText("Success")
                                            .setContentText("Card  successfully Formatted")
                                            .setConfirmText("Ok")
                                            .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                                @Override
                                                public void onClick(SweetAlertDialog sDialog) {
                                                    sDialog.dismissWithAnimation();

                                                }
                                            })
                                            .show();
                                }
                            });


                        } catch (final Exception err) {
                            Format_card.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pDialog.dismiss();
                                    aflag = false;
                                    err.printStackTrace();
                                    new SweetAlertDialog(Format_card.this, SweetAlertDialog.ERROR_TYPE)
                                            .setTitleText("Error")
                                            .setContentText("Card not Formatted")
                                            .setConfirmText("Ok")
                                            .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                                @Override
                                                public void onClick(SweetAlertDialog sDialog) {
                                                    sDialog.dismissWithAnimation();

                                                }
                                            })
                                            .show();
                                }
                            });
                        }
                    }
                }).start();


            } else if (result.equalsIgnoreCase("0")) {
                aflag = false;
                pDialogg.dismiss();
                new SweetAlertDialog(Format_card.this, SweetAlertDialog.WARNING_TYPE)
                        .setTitleText("Error")
                        .setContentText("Error Formatting this card")
                        .setConfirmText("OK")
                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sDialog) {
                                sDialog.dismissWithAnimation();

                            }
                        })
                        .show();
            }

        }
    }



}
