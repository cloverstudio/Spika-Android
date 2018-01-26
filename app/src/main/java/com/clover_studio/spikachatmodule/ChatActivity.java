package com.clover_studio.spikachatmodule;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.clover_studio.spikachatmodule.adapters.MessageRecyclerViewAdapter;
import com.clover_studio.spikachatmodule.adapters.SettingsAdapter;
import com.clover_studio.spikachatmodule.base.BaseActivity;
import com.clover_studio.spikachatmodule.base.SpikaApp;
import com.clover_studio.spikachatmodule.dialogs.BackgroundDialog;
import com.clover_studio.spikachatmodule.dialogs.DownloadFileDialog;
import com.clover_studio.spikachatmodule.dialogs.NotifyDialog;
import com.clover_studio.spikachatmodule.dialogs.PreviewAudioDialog;
import com.clover_studio.spikachatmodule.dialogs.PreviewMessageDialog;
import com.clover_studio.spikachatmodule.dialogs.PreviewPhotoDialog;
import com.clover_studio.spikachatmodule.dialogs.PreviewVideoDialog;
import com.clover_studio.spikachatmodule.dialogs.UploadFileDialog;
import com.clover_studio.spikachatmodule.models.Config;
import com.clover_studio.spikachatmodule.models.GetMessagesModel;
import com.clover_studio.spikachatmodule.models.LocationModel;
import com.clover_studio.spikachatmodule.models.Login;
import com.clover_studio.spikachatmodule.models.Message;
import com.clover_studio.spikachatmodule.models.SendTyping;
import com.clover_studio.spikachatmodule.models.UploadFileResult;
import com.clover_studio.spikachatmodule.models.User;
import com.clover_studio.spikachatmodule.robospice.api.DownloadFileManager;
import com.clover_studio.spikachatmodule.robospice.api.LoginApi;
import com.clover_studio.spikachatmodule.robospice.api.MessagesApi;
import com.clover_studio.spikachatmodule.robospice.api.UploadFileManagement;
import com.clover_studio.spikachatmodule.robospice.spice.CustomSpiceListener;
import com.clover_studio.spikachatmodule.utils.AnimUtils;
import com.clover_studio.spikachatmodule.utils.BuildTempFileAsync;
import com.clover_studio.spikachatmodule.utils.Const;
import com.clover_studio.spikachatmodule.utils.EmitJsonCreator;
import com.clover_studio.spikachatmodule.utils.LogCS;
import com.clover_studio.spikachatmodule.utils.OpenDownloadedFile;
import com.clover_studio.spikachatmodule.utils.SeenByUtils;
import com.clover_studio.spikachatmodule.view.menu.MenuManager;
import com.clover_studio.spikachatmodule.view.menu.OnMenuButtonsListener;
import com.clover_studio.spikachatmodule.view.menu.OnMenuManageListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.engineio.client.Transport;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.clover_studio.spikachatmodule.utils.Tools;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;


public class ChatActivity extends BaseActivity {

    private User activeUser;
    private Socket mSocket;

    private ListView settingsListView;
    protected RecyclerView rvMessages;
    protected TextView tvTyping;

    private EditText etMessage;
    private ImageButton btnSend;
    private ButtonType buttonType = ButtonType.MENU;
    private TypingType typingType = TypingType.BLANK;

    protected MenuManager menuManager;
    protected List<String> sentMessages = new ArrayList<>();
    protected List<User> typingUsers = new ArrayList<>();

    //data from last paging
    protected List<Message> lastDataFromServer = new ArrayList<>();

    //for scroll when keyboard opens
    protected int lastVisibleItem = 0;

    //boolean for open message information after keyboard is hidden
    private boolean openMessInfoAfterKeyboardHidden = false;
    private Message infoMessage = null;

    public enum ButtonType{
        MENU, SEND, MENU_OPENED, IN_ANIMATION;
    }

    public enum TypingType {
        TYPING, BLANK;
    }

    /**
     * start chat activity with user data
     * @param context
     * @param user user to login
     */
    public static void starChatActivity(Context context, User user){
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(Const.Extras.USER, user);
        context.startActivity(intent);
    }

    public static void startChatActivityWithConfig(Context context, User user, Config config){
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(Const.Extras.USER, user);
        intent.putExtra(Const.Extras.CONFIG, config);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        if(getIntent().hasExtra(Const.Extras.CONFIG)){
            Config config = getIntent().getParcelableExtra(Const.Extras.CONFIG);
            SpikaApp.getSharedPreferences().setConfig(config);
            SpikaApp.setConfig(config);
        }else{
            Config config = new Config("", "");
            SpikaApp.getSharedPreferences().setConfig(config);
            SpikaApp.setConfig(null);
        }

        setToolbar(R.id.tToolbar, R.layout.custom_chat_toolbar);
        setMenuLikeBack();
        onSettingsButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSettingsClicked();
            }
        });

        rvMessages = (RecyclerView) findViewById(R.id.rvMain);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));

        settingsListView = (ListView) findViewById(R.id.settings_list_view);
        SettingsAdapter adapter = new SettingsAdapter(this);
        settingsListView.setAdapter(adapter);
        adapter.setSettings();
        settingsListView.setOnItemClickListener(onSettingItemClick);

        btnSend = (ImageButton) findViewById(R.id.btnSend);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSendMenuButtonClicked();
            }
        });

        etMessage = (EditText) findViewById(R.id.etMessage);
        etMessage.addTextChangedListener(etMessageTextWatcher);

        tvTyping = (TextView) findViewById(R.id.typingTextView);

        menuManager = new MenuManager();
        menuManager.setMenuLayout(this, R.id.menuMain, onMenuManagerListener, onMenuButtonsListener);

        //check for user
        if(!getIntent().hasExtra(Const.Extras.USER)){
            noUserDialog();
            return;
        }else{
            activeUser = getIntent().getParcelableExtra(Const.Extras.USER);
            if(activeUser == null){
                noUserDialog();
                return;
            }
        }

        rvMessages.setAdapter(new MessageRecyclerViewAdapter(new ArrayList<Message>(), activeUser));
        ((MessageRecyclerViewAdapter)rvMessages.getAdapter()).setLastItemListener(onLastItemAndClickItemListener);

        setToolbarTitle(activeUser.roomID);

        findViewById(R.id.viewForSettingBehind).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSettings();
            }
        });

        findViewById(R.id.viewForMenuBehind).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonMenuOpenedClicked();
            }
        });

        login(activeUser);

        attachKeyboardListeners();

        rvMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                lastVisibleItem = ((LinearLayoutManager) rvMessages.getLayoutManager()).findLastVisibleItemPosition();
            }
        });

    }

    @Override
    protected void onShowKeyboard(int keyboardHeight) {
        super.onShowKeyboard(keyboardHeight);
        rvMessages.smoothScrollToPosition(lastVisibleItem);
    }

    @Override
    protected void onHideKeyboard() {
        super.onHideKeyboard();
        if(openMessInfoAfterKeyboardHidden){
            openMessInfoAfterKeyboardHidden = false;
            openMessageInfoDialog(infoMessage);
        }
    }

    protected OnMenuManageListener onMenuManagerListener = new OnMenuManageListener() {
        @Override
        public void onMenuOpened() {
            buttonType = ChatActivity.ButtonType.MENU_OPENED;
        }

        @Override
        public void onMenuClosed() {
            buttonType = ButtonType.MENU;
            etMessage.setEnabled(true);
            findViewById(R.id.viewForMenuBehind).setVisibility(View.GONE);
        }
    };

    protected OnMenuButtonsListener onMenuButtonsListener = new OnMenuButtonsListener() {
        @Override
        public void onCameraClicked() {
            CameraPhotoPreviewActivity.starCameraPhotoPreviewActivity(getActivity());
            onButtonMenuOpenedClicked();
        }

        @Override
        public void onAudioClicked() {
            RecordAudioActivity.starRecordAudioActivity(getActivity());
            onButtonMenuOpenedClicked();
        }

        @Override
        public void onFileClicked() {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, Const.RequestCode.PICK_FILE);
            onButtonMenuOpenedClicked();
        }

        @Override
        public void onVideoClicked() {
            RecordVideoActivity.starVideoPreviewActivity(getActivity());
            onButtonMenuOpenedClicked();
        }

        @Override
        public void onLocationClicked() {
            LocationActivity.startLocationActivity(getActivity());
            onButtonMenuOpenedClicked();
        }

        @Override
        public void onGalleryClicked() {
            CameraPhotoPreviewActivity.starCameraFromGalleryPhotoPreviewActivity(getActivity());
            onButtonMenuOpenedClicked();
        }
    };

    private AdapterView.OnItemClickListener onSettingItemClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if(position == 0){
                UsersInChatActivity.starUsersInChatActivity(getActivity(), activeUser.roomID);
            }else if(position == 1){
                BackgroundDialog.startDialog(getActivity(), new BackgroundDialog.OnBackgroundSelected() {
                    @Override
                    public void onSelected(int drawable, Dialog dialog) {
                        findViewById(R.id.parentView).setBackgroundResource(drawable);
                        dialog.dismiss();
                    }
                });
            }else if(position == 2){
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                startActivityForResult(intent, Const.RequestCode.CONTACT_CHOOSE);
            }
            hideSettings();
        }
    };

    protected TextWatcher etMessageTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s) {
            if(s.length() == 0){
                animateSendButton(false);
            }else{
                animateSendButton(true);
            }
            sendTypingType(s.length());
        }

    };

    /**
     * login user api
     * @param user user to login
     */
    private void login(User user) {
        handleProgress(true);
        LoginApi.Login spice = new LoginApi.Login(user);

        getSpiceManager().execute(spice, new CustomSpiceListener<Login>(this) {

            @Override
            public void onRequestSuccess(Login result) {
                doNotHideProgressNow = true;
                super.onRequestSuccess(result);
                if (result.success == 1) {
                    SpikaApp.getSharedPreferences().setToken(result.result.token);
                    SpikaApp.getSharedPreferences().setUserId(result.result.token);

                    if(TextUtils.isEmpty(activeUser.avatarURL)){
                        activeUser.avatarURL = result.result.user.avatarURL;
                    }
                    connectToSocket();
                }
            }
        });
    }

    /**
     * get messages from server
     * @param isInit true - initial call, false call on paging or on resume
     * @param lastMessageId id of last message (for paging can be null)
     */
    private void getMessages(final boolean isInit, final String lastMessageId){
        handleProgress(true);
        MessagesApi.GetMessages spice = new MessagesApi.GetMessages(activeUser.roomID, lastMessageId);

        getSpiceManager().execute(spice, new CustomSpiceListener<GetMessagesModel>(this) {

            @Override
            public void onRequestSuccess(GetMessagesModel result) {
                super.onRequestSuccess(result);
                if (result.success == 1) {
                    lastDataFromServer.clear();
                    lastDataFromServer.addAll(result.result);
                    MessageRecyclerViewAdapter adapter = (MessageRecyclerViewAdapter) rvMessages.getAdapter();
                    if (isInit) {
                        adapter.clearMessages();
                        lastVisibleItem = result.result.size();
                    }
                    adapter.addMessages(result.result);
                    if (isInit) {
                        scrollRecyclerToBottom();
                    } else {
                        int scrollToPosition = lastDataFromServer.size();
                        scrollRecyclerToPosition(scrollToPosition);
                    }

                    List<String> unReadMessages = SeenByUtils.getUnSeenMessages(result.result, activeUser);
                    sendOpenMessage(unReadMessages);

                }
            }
        });
    }

    protected MessageRecyclerViewAdapter.OnLastItemAndOnClickListener onLastItemAndClickItemListener = new MessageRecyclerViewAdapter.OnLastItemAndOnClickListener() {
        @Override
        public void onLastItem() {
            if(lastDataFromServer.size() < 50){
                //no more paging
                LogCS.e("LOG", "NO MORE MESSAGES");
            }else{
                if(lastDataFromServer.size() > 0){
                    String lastMessageId = lastDataFromServer.get(lastDataFromServer.size() - 1)._id;
                    boolean isInit = false;
                    getMessages(isInit, lastMessageId);
                }
            }
        }

        @Override
        public void onClickItem(final Message item) {
            if(item.deleted != -1 && item.deleted != 0){
                return;
            }
            if(item.type == Const.MessageType.TYPE_FILE){
                if(Tools.isMimeTypeImage(item.file.file.mimeType)){
                    PreviewPhotoDialog.startDialog(getActivity(), Tools.getFileUrlFromId(item.file.file.id));
                }else if(Tools.isMimeTypeVideo(item.file.file.mimeType)){
                    PreviewVideoDialog.startDialog(getActivity(), item.file);
                }else if(Tools.isMimeTypeAudio(item.file.file.mimeType)){
                    PreviewAudioDialog.startDialog(getActivity(), item.file);
                }else{
                    downloadFile(item);
                }
            }else if(item.type == Const.MessageType.TYPE_LOCATION){
                LocationActivity.startShowLocationActivity(getActivity(), item.location.lat, item.location.lng);
            }else if(item.type == Const.MessageType.TYPE_CONTACT){
                OpenDownloadedFile.selectedContactDialog(item.message, getActivity());
            }else{
                if(isKeyboardShowed){
                    openMessInfoAfterKeyboardHidden = true;
                    infoMessage = item;
                    hideKeyboard(etMessage);
                    return;
                }
                openMessageInfoDialog(item);
            }
        }

        @Override
        public void onInfoClick(final Message item) {
            if(isKeyboardShowed){
                openMessInfoAfterKeyboardHidden = true;
                infoMessage = item;
                hideKeyboard(etMessage);
                return;
            }
            openMessageInfoDialog(item);
        }
    };

    private void openMessageInfoDialog(Message message){
        PreviewMessageDialog dialog = PreviewMessageDialog.startDialog(getActivity(), message, activeUser, new PreviewMessageDialog.OnDeleteMessage() {
            @Override
            public void onDeleteMessage(Message message, Dialog dialog) {
                confirmDeleteMessage(message);
            }
        });
    }

    private void confirmDeleteMessage(final Message message){
        NotifyDialog dialog = NotifyDialog.startConfirm(getActivity(), getString(R.string.delete_message_title), getString(R.string.delete_message_text));
        dialog.setTwoButtonListener(new NotifyDialog.TwoButtonDialogListener() {
            @Override
            public void onOkClicked(NotifyDialog dialog) {
                dialog.dismiss();
                sendDeleteMessage(message._id);
            }

            @Override
            public void onCancelClicked(NotifyDialog dialog) {
                dialog.dismiss();
            }
        });
        dialog.setButtonsText(getString(R.string.NO_CAPITAL), getString(R.string.YES_CAPITAL));
    }

    private void animateSendButton(final boolean toSend){
        if(toSend && buttonType == ButtonType.SEND){
            return;
        }
        if(toSend){
            buttonType = ButtonType.SEND;
        }else{
            buttonType = ButtonType.MENU;
        }
        AnimUtils.fade(btnSend, 1, 0, 100, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (toSend) {
                    btnSend.setImageResource(R.drawable.ic_send);
                } else {
                    btnSend.setImageResource(R.drawable.ic_plus);
                }
                AnimUtils.fade(btnSend, 0, 1, 100, null);
            }
        });
    }

    protected void onSendMenuButtonClicked(){
        if(buttonType == ButtonType.MENU){
            onButtonMenuClicked();
        }else if(buttonType == ButtonType.MENU_OPENED){
            onButtonMenuOpenedClicked();
        }else if(buttonType == ButtonType.SEND){
            onButtonSendClicked();
        }
    }

    private void onButtonMenuClicked() {
        if(buttonType == ButtonType.IN_ANIMATION){
            return;
        }
        etMessage.setEnabled(false);
        buttonType = ButtonType.IN_ANIMATION;
        AnimUtils.rotateX(btnSend, 0, 45f, Const.AnimationDuration.MENU_BUTTON_ANIMATION_DURATION, null);

        menuManager.openMenu(btnSend);
        findViewById(R.id.viewForMenuBehind).setVisibility(View.VISIBLE);
    }

    private void onButtonMenuOpenedClicked() {
        if(buttonType == ButtonType.IN_ANIMATION){
            return;
        }
        buttonType = ButtonType.IN_ANIMATION;
        AnimUtils.rotateX(btnSend, 45f, 0, Const.AnimationDuration.MENU_BUTTON_ANIMATION_DURATION, null);

        menuManager.closeMenu();
    }

    protected void onButtonSendClicked(){
        sendMessage();
    }

    private void showSettings() {
        settingsListView.setVisibility(View.VISIBLE);
        AnimUtils.fade(settingsListView, 0, 1, 150, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                findViewById(R.id.viewForSettingBehind).setVisibility(View.VISIBLE);
            }
        });
    }

    private void hideSettings(){
        AnimUtils.fade(settingsListView, 1, 0, 150, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                settingsListView.setVisibility(View.INVISIBLE);
                findViewById(R.id.viewForSettingBehind).setVisibility(View.GONE);
            }
        });
    }

    protected void onSettingsClicked(){
        if(settingsListView.getVisibility() == View.VISIBLE){
            hideSettings();
        }else{
            showSettings();
        }
    }

    private void scrollRecyclerToBottom(){
        rvMessages.scrollToPosition(rvMessages.getAdapter().getItemCount() - 1);
    }

    private void scrollRecyclerToPosition(int pos){
        int offset = getResources().getDisplayMetrics().heightPixels;
        ((LinearLayoutManager)rvMessages.getLayoutManager()).scrollToPositionWithOffset(pos, 0);
    }

    //*********** send message to socket method

    /**
     * emit message to socket
     *
     * @param emitType type of emit message
     * @param jsonObject data for send to server
     */
    protected void emitMessage(String emitType, JSONObject jsonObject){
        mSocket.emit(emitType, jsonObject);
    }

    /**
     * send message type text
     */
    protected void sendMessage(){

        Message message = new Message();
        message.userID = activeUser.userID;
        message.roomID = activeUser.roomID;
        message.localID = Tools.generateRandomString(32);
        message.type = Const.MessageType.TYPE_TEXT;
        message.status = Const.MessageStatus.SENT;
        message.message = etMessage.getText().toString();
        message.created = System.currentTimeMillis();

        etMessage.setText("");

        JSONObject emitMessage = EmitJsonCreator.createEmitSendMessage(message);
        emitMessage(Const.EmitKeyWord.SEND_MESSAGE, emitMessage);

        onMessageSent(message);

    }

    /**
     * send message type file
     * @param result upload file data
     */
    protected void sendFile(UploadFileResult result){
        Message message = new Message();
        message.userID = activeUser.userID;
        message.roomID = activeUser.roomID;
        message.localID = Tools.generateRandomString(32);
        message.type = Const.MessageType.TYPE_FILE;
        message.status = Const.MessageStatus.SENT;
        message.message = "";
        message.file = result.result;

        etMessage.setText("");

        JSONObject emitMessage = EmitJsonCreator.createEmitSendMessage(message);

        emitMessage(Const.EmitKeyWord.SEND_MESSAGE, emitMessage);

        onMessageSent(message);

    }

    /**
     * send message type location
     * @param address location address
     * @param latLng location latitude and longitude
     */
    private void sendLocation(String address, LatLng latLng) {

        Message message = new Message();
        message.userID = activeUser.userID;
        message.roomID = activeUser.roomID;
        message.localID = Tools.generateRandomString(32);
        message.type = Const.MessageType.TYPE_LOCATION;
        message.status = Const.MessageStatus.SENT;
        message.message = address;

        LocationModel location = new LocationModel();
        location.lat = latLng.latitude;
        location.lng = latLng.longitude;
        message.location = location;

        etMessage.setText("");

        JSONObject emitMessage = EmitJsonCreator.createEmitSendMessage(message);
        emitMessage(Const.EmitKeyWord.SEND_MESSAGE, emitMessage);

        onMessageSent(message);

    }

    /**
     * send contact
     * @param name name of contact
     * @param vCardLikeString vCard in string format
     */
    protected void sendContact(String name, String vCardLikeString){
        Message message = new Message();
        message.userID = activeUser.userID;
        message.roomID = activeUser.roomID;
        message.localID = Tools.generateRandomString(32);
        message.type = Const.MessageType.TYPE_CONTACT;
        message.status = Const.MessageStatus.SENT;
        message.message = vCardLikeString;

        etMessage.setText("");

        JSONObject emitMessage = EmitJsonCreator.createEmitSendMessage(message);
        emitMessage(Const.EmitKeyWord.SEND_MESSAGE, emitMessage);

        onMessageSent(message);

    }

    private void loginWithSocket(){
        JSONObject emitLogin = EmitJsonCreator.createEmitLoginMessage(activeUser);
        emitMessage(Const.EmitKeyWord.LOGIN, emitLogin);
    }

    private void sendTypingType(int length) {
        if(length > 0 && typingType == TypingType.BLANK){
            setTyping(Const.TypingStatus.TYPING_ON);
            typingType = TypingType.TYPING;
        }else if(length == 0 && typingType == TypingType.TYPING){
            setTyping(Const.TypingStatus.TYPING_OFF);
            typingType = TypingType.BLANK;
        }
    }

    private void setTyping(int type){
        JSONObject emitSendTyping = EmitJsonCreator.createEmitSendTypingMessage(activeUser, type);
        emitMessage(Const.EmitKeyWord.SEND_TYPING, emitSendTyping);
    }

    private void sendOpenMessage(String messageId){
        List<String> messagesIds = new ArrayList<>();
        messagesIds.add(messageId);
        sendOpenMessage(messagesIds);
    }

    private void sendOpenMessage(List<String> messagesIds){
        JSONObject emitOpenMessage = EmitJsonCreator.createEmitOpenMessage(messagesIds, activeUser.userID);
        emitMessage(Const.EmitKeyWord.OPEN_MESSAGE, emitOpenMessage);
    }

    protected void sendDeleteMessage(String messageId){
        JSONObject emitDeleteMessage = EmitJsonCreator.createEmitDeleteMessage(activeUser.userID, messageId);
        emitMessage(Const.EmitKeyWord.DELETE_MESSAGE, emitDeleteMessage);
    }
    //****************************************************

    //on received message from socket

    private void onUserLeft(User user) {
        if(typingUsers.contains(user)){
            typingUsers.remove(user);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (typingUsers.size() < 1) {
                        tvTyping.setVisibility(View.GONE);
                        tvTyping.setText("");
                    } else {
                        generateTypingString();
                    }
                }
            });
        }
    }

    private void onMessageSent(Message sendMessage) {
        MessageRecyclerViewAdapter adapter = (MessageRecyclerViewAdapter) rvMessages.getAdapter();
        adapter.addSentMessage(sendMessage);
        sentMessages.add(sendMessage.localID);
        lastVisibleItem = adapter.getItemCount();
        scrollRecyclerToBottom();
    }

    private void onMessageReceived(final Message message){
        final MessageRecyclerViewAdapter adapter = (MessageRecyclerViewAdapter) rvMessages.getAdapter();
        if(sentMessages.contains(message.localID)){
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.setDeliveredMessage(message);
                }
            });
            sentMessages.remove(message.localID);
        }else{
            message.status = Const.MessageStatus.RECEIVED;
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.addReceivedMessage(message);
                    if(!message.user.userID.equals(activeUser.userID)){
                        sendOpenMessage(message._id);
                    }
                }
            });
        }
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lastVisibleItem = rvMessages.getAdapter().getItemCount();
                scrollRecyclerToBottom();
            }
        });
    }

    private void onMessagesUpdated(final List<Message> messages) {

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MessageRecyclerViewAdapter adapter = (MessageRecyclerViewAdapter) rvMessages.getAdapter();
                adapter.updateMessages(messages);
            }
        });

    }

    private void onTyping(final SendTyping typing){
        if(typing.user.userID.equals(activeUser.userID)){
            return;
        }

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (typing.type == Const.TypingStatus.TYPING_OFF) {

                    if(typingUsers.contains(typing.user)){
                        typingUsers.remove(typing.user);
                    }

                    if (typingUsers.size() < 1) {
                        tvTyping.setVisibility(View.GONE);
                        tvTyping.setText("");
                    } else {
                        generateTypingString();
                    }
                } else {

                    if(typingUsers.contains(typing.user)){
                        typingUsers.remove(typing.user);
                    }

                    tvTyping.setVisibility(View.VISIBLE);
                    scrollRecyclerToBottom();
                    typingUsers.add(typing.user);
                    generateTypingString();
                }
            }
        });
    }

    private void checkToRemoveUser(User user){
        for(User item : typingUsers) {
            if(item.userID.equals(user.userID)){
                typingUsers.remove(item);
                return;
            }
        }
    }

    //******************************************

    /**
     * connect to socket
     */
    private void connectToSocket(){

        try {

            IO.Options opts = new IO.Options();
            opts.forceNew = true;
            mSocket = IO.socket(SpikaApp.getConfig().socketUrl, opts);
            mSocket.connect();

        } catch (URISyntaxException e) {
            e.printStackTrace();
            socketFailedDialog();
            return;
        }

        loginWithSocket();

        mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                LogCS.w("LOG", "CONNECTED TO SOCKET");
            }
        });

        mSocket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                socketFailedDialog();
            }
        });

        mSocket.on(Socket.EVENT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                socketFailedDialog();
            }
        });

        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                socketFailedDialog();
            }
        });

        mSocket.on(Const.EmitKeyWord.NEW_USER, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.w("LOG", "new user, args" + args[0].toString());
            }
        });

        mSocket.on(Const.EmitKeyWord.USER_LEFT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String userLeft = args[0].toString();
                ObjectMapper mapper = new ObjectMapper();
                try {
                    User user = mapper.readValue(userLeft, User.class);
                    onUserLeft(user);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        mSocket.on(Const.EmitKeyWord.SEND_TYPING, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String sendTyping = args[0].toString();
                ObjectMapper mapper = new ObjectMapper();
                try {
                    SendTyping typing = mapper.readValue(sendTyping, SendTyping.class);
                    onTyping(typing);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        mSocket.on(Const.EmitKeyWord.NEW_MESSAGE, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String newMessage = args[0].toString();
                ObjectMapper mapper = new ObjectMapper();
                try {
                    Message message = mapper.readValue(newMessage, Message.class);
                    onMessageReceived(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        mSocket.on(Const.EmitKeyWord.MESSAGE_UPDATED, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String newMessage = args[0].toString();
                ObjectMapper mapper = new ObjectMapper();
                try {
                    List<Message> messages = mapper.readValue(newMessage, mapper.getTypeFactory().constructCollectionType(List.class, Message.class));
                    onMessagesUpdated(messages);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        //progress is visible, and it is showed in login method
        doNotShowProgressNow = true;
        boolean isInit = true; String lastMessageId = null;
        getMessages(isInit, lastMessageId);

    }

    private void generateTypingString(){
        String typingText = "";
        for(User item: typingUsers){
            typingText = typingText + item.name + ", ";
        }
        typingText = typingText.substring(0, typingText.length() - 2);

        if(typingUsers.size() > 1 ){
            tvTyping.setText(typingText + " " + getString(R.string.are_typing));
        }else{
            tvTyping.setText(typingText + " " + getString(R.string.is_typing));
        }
    }

    protected void noUserDialog(){
        NotifyDialog dialog = NotifyDialog.startInfo(this, getString(R.string.user_error_title), getString(R.string.user_error_not_sent));
        dialog.setOneButtonListener(new NotifyDialog.OneButtonDialogListener() {
            @Override
            public void onOkClicked(NotifyDialog dialog) {
                dialog.dismiss();
                finish();
            }
        });
    }

    protected void socketFailedDialog(){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                NotifyDialog dialog = NotifyDialog.startInfo(getActivity(), getString(R.string.socket_error_title), getString(R.string.socket_error_connect_failed));
                dialog.setOneButtonListener(new NotifyDialog.OneButtonDialogListener() {
                    @Override
                    public void onOkClicked(NotifyDialog dialog) {
                        dialog.dismiss();
                        finish();
                    }
                });
            }
        });
    }

    @Override
    public void onBackPressed() {
        if(settingsListView.getVisibility() == View.VISIBLE){
            hideSettings();
            return;
        }
        if(buttonType == ButtonType.MENU_OPENED){
            onButtonMenuOpenedClicked();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if(mSocket != null){
            mSocket.close();
            mSocket.disconnect();
            mSocket = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == Const.RequestCode.PHOTO_CHOOSE){
            if(resultCode == RESULT_OK){
                if(data != null && data.getExtras().containsKey(Const.Extras.UPLOAD_MODEL)){
                    UploadFileResult model = data.getExtras().getParcelable(Const.Extras.UPLOAD_MODEL);
                    sendFile(model);
                }
            }
        }else if(requestCode == Const.RequestCode.PICK_FILE){
            if(resultCode == RESULT_OK){
                getFile(data);
            }
        }else if(requestCode == Const.RequestCode.VIDEO_CHOOSE){
            if(resultCode == RESULT_OK){
                if(data != null && data.getExtras().containsKey(Const.Extras.UPLOAD_MODEL)){
                    UploadFileResult model = data.getExtras().getParcelable(Const.Extras.UPLOAD_MODEL);
                    sendFile(model);
                }
            }
        }else if(requestCode == Const.RequestCode.AUDIO_CHOOSE){
            if(resultCode == RESULT_OK){
                if(data != null && data.getExtras().containsKey(Const.Extras.UPLOAD_MODEL)){
                    UploadFileResult model = data.getExtras().getParcelable(Const.Extras.UPLOAD_MODEL);
                    sendFile(model);
                }
            }
        }else if(requestCode == Const.RequestCode.LOCATION_CHOOSE){
            if(resultCode == RESULT_OK){
                if(data != null && data.getExtras().containsKey(Const.Extras.LATLNG)){
                    String address = null;
                    if(data.getExtras().containsKey(Const.Extras.ADDRESS)){
                        address = data.getExtras().getString(Const.Extras.ADDRESS);
                    }
                    LatLng latLng = data.getExtras().getParcelable(Const.Extras.LATLNG);
                    sendLocation(address, latLng);
                }
            }
        }else if(requestCode == Const.RequestCode.CONTACT_CHOOSE){
            if(resultCode == RESULT_OK){
                Uri contactData = data.getData();
                Cursor cursor = getContentResolver().query(contactData, null, null, null, null);
                try {
                    int nameColumn = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                    if (cursor.moveToFirst()) {
                        String name = cursor.getString(nameColumn);
                        String lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);
                        AssetFileDescriptor fd;
                        fd = getContentResolver().openAssetFileDescriptor(uri, "r");
                        FileInputStream fis = fd.createInputStream();
                        byte[] b = new byte[(int) fd.getDeclaredLength()];
                        fis.read(b);
                        String vCard = new String(b);

                        sendContact(name, vCard);
                    } else {
                        NotifyDialog.startInfo(getActivity(), getString(R.string.contact_error_title), getString(R.string.contact_error_select));
                    }
                    cursor.close();
                }catch (Exception ex){
                    cursor.close();
                    NotifyDialog.startInfo(getActivity(), getString(R.string.contact_error_title), getString(R.string.contact_error_select));
                }


            }
        }
    }

    //************** ui customization methods
    protected void changeToolbarColor(String color){
        super.changeToolbarColor(color);
    }
    //******************************************

    //************** download and upload file

    private void getFile(Intent data) {
        Uri fileUri = data.getData();

        String fileName = null;
        String filePath = null;

        if (fileUri.getScheme().equals("content")) {

            String proj[];
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                proj = new String[] { MediaStore.Files.FileColumns.DISPLAY_NAME };
            } else {
                proj = new String[] { MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.DISPLAY_NAME };
            }
            Cursor cursor = getContentResolver().query(fileUri, proj, null, null, null);
            cursor.moveToFirst();

            int column_index_name = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME);
            int column_index_path = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);

            fileName = cursor.getString(column_index_name);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                try {
                    new BuildTempFileAsync(this, fileName, new BuildTempFileAsync.OnTempFileCreatedListener() {
                        @Override
                        public void onTempFileCreated(String path, String name) {
                            if (TextUtils.isEmpty(path)) {
                                onFileSelected(RESULT_CANCELED, null, null);
                            } else {
                                onFileSelected(RESULT_OK, name, path);
                            }
                        }
                    }).execute(getContentResolver().openInputStream(fileUri));
                    // async task initialized, exit
                    return;
                } catch (FileNotFoundException ignored) {
                    filePath = "";
                }
            } else {
                filePath = cursor.getString(column_index_path);
            }

        } else if (fileUri.getScheme().equals("file")) {

            File file = new File(URI.create(fileUri.toString()));
            fileName = file.getName();
            filePath = file.getAbsolutePath();

            if (!TextUtils.isEmpty(fileName) && !TextUtils.isEmpty(filePath)) {
                onFileSelected(RESULT_OK, fileName, filePath);
            } else {
                onFileSelected(RESULT_CANCELED, null, null);
            }
        }

    }

    private void onFileSelected(int resultOk, String fileName, String filePath) {
        if(resultOk == RESULT_OK){
            uploadFile(fileName, filePath);
        }
    }

    private void uploadFile(String fileName, String filePath) {

        String mimeType = Tools.getMimeType(filePath);
        if(TextUtils.isEmpty(mimeType)){
            mimeType = Const.ContentTypes.OTHER;
        }

        final UploadFileDialog dialog = UploadFileDialog.startDialog(getActivity());

        UploadFileManagement tt = new UploadFileManagement();
        tt.new BackgroundUploader(SpikaApp.getConfig().apiBaseUrl + Const.Api.UPLOAD_FILE, new File(filePath), mimeType, new UploadFileManagement.OnUploadResponse() {
            @Override
            public void onStart() {
                LogCS.d("LOG", "START UPLOADING");
            }

            @Override
            public void onSetMax(final int max) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.setMax(max);
                    }
                });
            }

            @Override
            public void onProgress(final int current) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.setCurrent(current);
                    }
                });
            }

            @Override
            public void onFinishUpload() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.fileUploaded();
                    }
                });
            }

            @Override
            public void onResponse(final boolean isSuccess, final String result) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        if(!isSuccess){
                            onResponseFailed();
                        }else{
                            onResponseFinish(result);
                        }
                    }
                });
            }
        }).execute();
    }

    private void onResponseFailed() {
        NotifyDialog.startInfo(getActivity(), getString(R.string.error), getString(R.string.file_not_found));
    }

    private void onResponseFinish(String result) {
        ObjectMapper mapper = new ObjectMapper();
        UploadFileResult data = null;
        try {
            data = mapper.readValue(result, UploadFileResult.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(data != null){
            sendFile(data);
        }
    }

    private void downloadFile(Message item) {

        File file = new File(Tools.getDownloadFolderPath() + "/" + item.created + item.file.file.name);

        if(file.exists()){
            OpenDownloadedFile.downloadedFileDialog(file, getActivity());
        }else{

            final DownloadFileDialog dialog = DownloadFileDialog.startDialog(getActivity());

            DownloadFileManager.downloadVideo(getActivity(), Tools.getFileUrlFromId(item.file.file.id), file, new DownloadFileManager.OnDownloadListener() {
                @Override
                public void onStart() {
                    LogCS.d("LOG", "START UPLOADING");
                }

                @Override
                public void onSetMax(final int max) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.setMax(max);
                        }
                    });
                }

                @Override
                public void onProgress(final int current) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.setCurrent(current);
                        }
                    });
                }

                @Override
                public void onFinishDownload() {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.fileDownloaded();
                        }
                    });
                }

                @Override
                public void onResponse(boolean isSuccess, final String path) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            OpenDownloadedFile.downloadedFileDialog(new File(path), getActivity());
                        }
                    });
                }
            });

        }

    }

    //************************************************

}
