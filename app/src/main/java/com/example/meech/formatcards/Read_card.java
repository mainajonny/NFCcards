package com.example.meech.formatcards;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.TextView;
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

import org.json.JSONObject;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class Read_card extends AppCompatActivity {

    private IKeyData objKEY_2KTDES_ULC = null;
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

    MaterialDialog pDialogg;

    JSONObject data =new JSONObject(),pdetails,card_pin;
    TextView cardno,name,bal,tap;
    ViewGroup details;

    Animation animation;
    Typeface typeface;
    String bgip="";
    Context context=this;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_card);
        /*Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);*/


        initializeLibrary();
        initializeKeys();
        initializeCipherinitVector();


        tap=(TextView)findViewById(R.id.tapcard);



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
        CardType type = CardType.UnknownCard;
        try {
            type = libInstance.getCardType(intent);
        } catch (NxpNfcLibException ex) {
            Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
        }

        switch (type) {

            case DESFireEV1: {
                mCardType = CardType.DESFireEV1;
                desFireEV1 = DESFireFactory.getInstance().getDESFire(libInstance.getCustomModules());
                try {

                    desFireEV1.getReader().connect();
                    desFireEV1.getReader().setTimeout(2000);
                    pDialogg = new MaterialDialog.Builder(this)
                            .title("Reading Card")
                            .content("Please don't remove the Card!")
                            .progress(true, 0)
                            .progressIndeterminateStyle(false)
                            .show();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            desfireEV1CardLogic();
                        }
                    }).start();

                } catch (Exception t) {
                    t.printStackTrace();
                    Log.d("##ERROR", t.toString());

                }
                break;
            }


        }

    }


    @TargetApi(19)
    private void initializeLibrary() {
        libInstance = NxpNfcLib.getInstance();
        try {
            libInstance.registerActivity(this, packageKey);
        } catch (NxpNfcLibException ex) {

        }
    }

    /**
     * Initialize the Cipher and init vector of 16 bytes with 0xCD.
     */


    private void desfireEV1CardLogic() {
        byte[] appId1 = new byte[]{0x2, 0x00, 0x00};
        int timeOut = 2000;

        try {
            desFireEV1.getReader().setTimeout(timeOut);
            desFireEV1.authenticate(0, IDESFireEV1.AuthType.Native, KeyType.TWO_KEY_THREEDES, objKEY_2KTDES);
            desFireEV1.selectApplication(0);
            desFireEV1.selectApplication(appId1);
            desFireEV1.authenticate(0, IDESFireEV1.AuthType.Native, KeyType.TWO_KEY_THREEDES, objKEY_2KTDES);

            pdetails =new JSONObject(new String(desFireEV1.readData(Card_Structure.PersonalDetails, 0, 0)));

            desFireEV1.getReader().close();


            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pDialogg.dismiss();
                    try {

                        new MaterialDialog.Builder(context)
                                .title(("Name")+": " + pdetails.getString("Name"))
                                .content(("ID:  ")+pdetails.getString("Id")
                                        +" "+  "Email:  "+pdetails.getString("Email")
                                        +" "+  "Phone:  "+pdetails.getString("Phone"))
                                .positiveText("Ok")
                                .show();

                    }catch (Exception e){
                        Log.e("######CardError", e.toString());

                    }
                }
            });

        }catch (final Exception e){
            Log.e("###error", e.toString());
            if(!(data.length()>0)){
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pDialogg.dismiss();
                        e.printStackTrace();
                        new SweetAlertDialog(getApplicationContext(), SweetAlertDialog.WARNING_TYPE)
                                .setTitleText("")
                                .setContentText("No Data Found on This Card")
                                .setConfirmText("OK")
                                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                    @Override
                                    public void onClick(SweetAlertDialog sDialog) {
                                        sDialog.dismissWithAnimation();

                                    }
                                })
                                .show();

                    }
                });
            }else{
                Log.i("#####ERROR",e.toString());
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pDialogg.dismiss();
                        new SweetAlertDialog(getApplicationContext(), SweetAlertDialog.ERROR_TYPE)
                                .setTitleText("Error")
                                .setContentText("Error Reading this card")
                                .setConfirmText("OK")
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
        //fetch the data

    }
    private void initializeKeys() {
        KeyInfoProvider infoProvider = KeyInfoProvider.getInstance(getApplicationContext());
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


}
