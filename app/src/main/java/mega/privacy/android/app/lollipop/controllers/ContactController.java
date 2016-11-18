package mega.privacy.android.app.lollipop.controllers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContact;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.listeners.ContactNameListener;
import mega.privacy.android.app.lollipop.listeners.MultipleRequestListener;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaUser;

public class ContactController {

    Context context;
    MegaApiAndroid megaApi;
    DatabaseHandler dbH;
    MegaPreferences prefs = null;

    public ContactController(Context context){
        log("ContactController created");
        this.context = context;
        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context);
        }
    }

    public void pickFolderToShare(List<MegaUser> users){

        Intent intent = new Intent(context, FileExplorerActivityLollipop.class);
        intent.setAction(FileExplorerActivityLollipop.ACTION_SELECT_FOLDER_TO_SHARE);
        ArrayList<String> longArray = new ArrayList<String>();
        for (int i=0; i<users.size(); i++){
            longArray.add(users.get(i).getEmail());
        }
        intent.putStringArrayListExtra("SELECTED_CONTACTS", longArray);
        ((ManagerActivityLollipop) context).startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_FOLDER);
    }

    public void pickFileToSend(List<MegaUser> users){
        log("pickFileToSend");
        Intent intent = new Intent(context, FileExplorerActivityLollipop.class);
        intent.setAction(FileExplorerActivityLollipop.ACTION_SELECT_FILE);
        ArrayList<String> longArray = new ArrayList<String>();
        for (int i=0; i<users.size(); i++){
            longArray.add(users.get(i).getEmail());
        }
        intent.putStringArrayListExtra("SELECTED_CONTACTS", longArray);
        ((ManagerActivityLollipop) context).startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_FILE);
    }

    public void removeContact(MegaUser c){
        log("removeContact");
        final ArrayList<MegaNode> inShares = megaApi.getInShares(c);
        if(inShares.size() != 0)
        {
            for(int i=0; i<inShares.size();i++){
                MegaNode removeNode = inShares.get(i);
                megaApi.remove(removeNode);
            }
        }

        megaApi.removeContact(c, (ManagerActivityLollipop) context);
    }


    public void removeMultipleContacts(final ArrayList<MegaUser> contacts){
        log("removeMultipleContacts");

        MultipleRequestListener removeMultipleListener = null;
        if(contacts.size()>1){
            log("remove multiple contacts");
            removeMultipleListener = new MultipleRequestListener(-1, context);
            for(int j=0; j<contacts.size();j++){

                final MegaUser c= contacts.get(j);

                final ArrayList<MegaNode> inShares = megaApi.getInShares(c);

                if(inShares.size() != 0){
                    for(int i=0; i<inShares.size();i++){
                        MegaNode removeNode = inShares.get(i);
                        megaApi.remove(removeNode);
                    }
                }
                megaApi.removeContact(c, removeMultipleListener);
            }
        }
        else{
            log("remove one contact");

            final MegaUser c= contacts.get(0);

            final ArrayList<MegaNode> inShares = megaApi.getInShares(c);

            if(inShares.size() != 0){
                for(int i=0; i<inShares.size();i++){
                    MegaNode removeNode = inShares.get(i);
                    megaApi.remove(removeNode);
                }
            }
            megaApi.removeContact(c, (ManagerActivityLollipop) context);
        }
    }

    public void reinviteMultipleContacts(final List<MegaContactRequest> requests){
        log("reinviteMultipleContacts");

        MultipleRequestListener reinviteMultipleListener = null;
        if(requests.size()>1){
            log("reinvite multiple request");
            reinviteMultipleListener = new MultipleRequestListener(-1, context);
            for(int j=0; j<requests.size();j++){

                final MegaContactRequest request= requests.get(j);

                megaApi.inviteContact(request.getTargetEmail(), null, MegaContactRequest.INVITE_ACTION_REMIND, reinviteMultipleListener);
            }
        }
        else{
            log("reinvite one request");

            final MegaContactRequest request= requests.get(0);

            reinviteContact(request);
        }
    }

    public void deleteMultipleSentRequestContacts(final List<MegaContactRequest> requests){
        log("deleteMultipleSentRequestContacts");

        MultipleRequestListener deleteMultipleListener = null;
        if(requests.size()>1){
            log("delete multiple request");
            deleteMultipleListener = new MultipleRequestListener(-1, context);
            for(int j=0; j<requests.size();j++){

                final MegaContactRequest request= requests.get(j);

                megaApi.inviteContact(request.getTargetEmail(), null, MegaContactRequest.INVITE_ACTION_DELETE, deleteMultipleListener);
            }
        }
        else{
            log("delete one request");

            final MegaContactRequest request= requests.get(0);

            removeInvitationContact(request);
        }
    }

    public void acceptMultipleReceivedRequest(final List<MegaContactRequest> requests){
        log("acceptMultipleReceivedRequest");

        MultipleRequestListener acceptMultipleListener = null;
        if(requests.size()>1){
            log("accept multiple request");
            acceptMultipleListener = new MultipleRequestListener(-1, context);
            for(int j=0; j<requests.size();j++){

                final MegaContactRequest request= requests.get(j);
                megaApi.replyContactRequest(request, MegaContactRequest.REPLY_ACTION_ACCEPT, acceptMultipleListener);
            }
        }
        else{
            log("accept one request");

            final MegaContactRequest request= requests.get(0);
            acceptInvitationContact(request);
        }
    }

    public void declineMultipleReceivedRequest(final List<MegaContactRequest> requests){
        log("declineMultipleReceivedRequest");

        MultipleRequestListener declineMultipleListener = null;
        if(requests.size()>1){
            log("decline multiple request");
            declineMultipleListener = new MultipleRequestListener(-1, context);
            for(int j=0; j<requests.size();j++){

                final MegaContactRequest request= requests.get(j);
                megaApi.replyContactRequest(request, MegaContactRequest.REPLY_ACTION_DENY, declineMultipleListener);
            }
        }
        else{
            log("decline one request");

            final MegaContactRequest request= requests.get(0);
            declineInvitationContact(request);
        }
    }

    public void ignoreMultipleReceivedRequest(final List<MegaContactRequest> requests){
        log("ignoreMultipleReceivedRequest");

        MultipleRequestListener ignoreMultipleListener = null;
        if(requests.size()>1){
            log("ignore multiple request");
            ignoreMultipleListener = new MultipleRequestListener(-1, context);
            for(int j=0; j<requests.size();j++){

                final MegaContactRequest request= requests.get(j);
                megaApi.replyContactRequest(request, MegaContactRequest.REPLY_ACTION_IGNORE, ignoreMultipleListener);
            }
        }
        else{
            log("ignore one request");

            final MegaContactRequest request= requests.get(0);
            ignoreInvitationContact(request);
        }
    }

    public void inviteContact(String contactEmail){
        log("inviteContact");

        if (!Util.isOnline(context)){
            ((ManagerActivityLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
            return;
        }

        if(((ManagerActivityLollipop) context).isFinishing()){
            return;
        }

        megaApi.inviteContact(contactEmail, null, MegaContactRequest.INVITE_ACTION_ADD, (ManagerActivityLollipop) context);
    }


    public void addContactDB(String email){
        log("addContactDB");

        MegaUser user = megaApi.getContact(email);
        if(user!=null){
            log("User to add: "+user.getEmail());
            //Check the user is not previously in the DB
            if(dbH.findContactByHandle(String.valueOf(user.getHandle()))==null){
                log("The contact NOT exists -> add to DB");
                MegaContact megaContact = new MegaContact(String.valueOf(user.getHandle()), user.getEmail(), "", "");
                dbH.setContact(megaContact);
                megaApi.getUserAttribute(user, 1, new ContactNameListener(context));
                megaApi.getUserAttribute(user, 2, new ContactNameListener(context));
            }
            else{
                log("The contact already exists -> update");
                megaApi.getUserAttribute(user, 1, new ContactNameListener(context));
                megaApi.getUserAttribute(user, 2, new ContactNameListener(context));
            }
        }
    }


    public void acceptInvitationContact(MegaContactRequest c){
        log("acceptInvitationContact");
        megaApi.replyContactRequest(c, MegaContactRequest.REPLY_ACTION_ACCEPT, (ManagerActivityLollipop) context);
    }

    public void declineInvitationContact(MegaContactRequest c){
        log("declineInvitationContact");
        megaApi.replyContactRequest(c, MegaContactRequest.REPLY_ACTION_DENY, (ManagerActivityLollipop) context);
    }

    public void ignoreInvitationContact(MegaContactRequest c){
        log("ignoreInvitationContact");
        megaApi.replyContactRequest(c, MegaContactRequest.REPLY_ACTION_IGNORE, (ManagerActivityLollipop) context);
    }

    public void reinviteContact(MegaContactRequest c){
        log("inviteContact");
        megaApi.inviteContact(c.getTargetEmail(), null, MegaContactRequest.INVITE_ACTION_REMIND, (ManagerActivityLollipop) context);
    }

    public void removeInvitationContact(MegaContactRequest c){
        log("removeInvitationContact");
        megaApi.inviteContact(c.getTargetEmail(), null, MegaContactRequest.INVITE_ACTION_DELETE, (ManagerActivityLollipop) context);
    }

    public static void log(String message) {
        Util.log("ContactController", message);
    }
}
