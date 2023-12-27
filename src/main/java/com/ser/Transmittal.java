package com.ser;
import com.ser.sedna.client.bluelineimpl.bpm.InformationObjectLinks;
import utils.*;

import com.ser.blueline.*;
import com.ser.blueline.bpm.IProcessInstance;
import com.ser.blueline.bpm.ITask;
import com.ser.blueline.bpm.ITaskDefinition;
import com.ser.evITAWeb.EvitaWebException;
import com.ser.evITAWeb.api.IDialog;
import com.ser.evITAWeb.api.actions.IBasicAction;
import com.ser.evITAWeb.api.actions.IMessageAction;
import com.ser.evITAWeb.api.actions.IStopFurtherAction;
import com.ser.evITAWeb.api.controls.IControl;
import com.ser.evITAWeb.api.controls.ITextField;
import com.ser.evITAWeb.api.options.EnumReadonlyMode;
import com.ser.evITAWeb.scripting.Doxis4ClassFactory;
import com.ser.evITAWeb.scripting.bpmservice.task.TaskScripting;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class Transmittal extends TaskScripting {


    private static Logger log;
    public ISession ses;
    private IDialog dlg;
    public Transmittal(ITask task){
        super(task);
        this.log=super.log;
    }

    @Override
    public void onInit() throws EvitaWebException {

        boolean isExternal = false;
        ses = getTask().getSession();



        ITaskDefinition taskDefinition = getTask().getTaskDefinition();
        String activityName = taskDefinition.getName();
        if (isNew() ) {

            IUnit uiExternal= ses.getDocumentServer().getUnitByName( ses,"ExternalReader");
            IUser usr = getTask().getCreator();
            if(usr==null) usr = ses.getUser();

            List<String> units = Arrays.asList(usr.getUnitIDs());
            isExternal =units.contains(uiExternal.getID()) ;

            processView.setDialogType("internal");
            if (isExternal)  processView.setDialogType("external");

        } else
            processView.setDialogType("default");


       // processView.setItemReadonlyMode(ProcessDisplayItem.DOCUMENT_LINKS, EnumReadonlyMode.READONLY);

    }

    @Override
    public IBasicAction onCommit(IDialog dialog) throws EvitaWebException {
        // Return, if descriptor value not set
        if (dialog != null) {

            IControl fieldByName = dialog.getFieldByName("ccmPRJCard_name");
            if (fieldByName != null && fieldByName instanceof ITextField) {
                ITextField textField = (ITextField) fieldByName;
                String value = textField.getText();
                if (value == null || "".equals(value)) {
                    IStopFurtherAction createStopFurtherAction = Doxis4ClassFactory.createStopFurtherAction();
                    createStopFurtherAction.setMessage("Invalid Project Name. Kindly re-check your inputs");
                    createStopFurtherAction.setType(IMessageAction.EnumMessageType.ERROR);
                    return createStopFurtherAction;
                }
            }
        }
        return null;
    }
@Override
    public void onInitMetadataDialog(IDialog dialog) throws EvitaWebException {
        this.dlg = dialog;
        ses = getTask().getSession();

        if(!isNew() ) return;
        IUnit uiExternal= ses.getDocumentServer().getUnitByName( ses,"ExternalReader");
        IUser usr = getTask().getCreator();
        if(usr==null) usr = ses.getUser();
        log.info("usr:" + usr.getLogin());

        boolean isExternal = false;
        List<String> units = Arrays.asList(usr.getUnitIDs());
        isExternal = units.contains(uiExternal.getID()) ;

        String mainCompName = GeneralLib.getMainCompGVList(ses,"CCM_PARAM_CONTRACTOR-MEMBERS","NAME");
        String mainCompSName = GeneralLib.getMainCompGVList(ses,"CCM_PARAM_CONTRACTOR-MEMBERS","SNAME");
        log.info("MAIN COM NAME:" + mainCompName);

        String from = "";
        String receiver = "";
        String ownerCompSName = "";
        String ownerCompName = "";
        if(isExternal) {
            IDocument ownerContactFile = GeneralLib.getContactRecord(ses, usr.getEMailAddress());
            log.info("external user email:" + usr.getEMailAddress());
            log.info("owner contact file:" + ownerContactFile);
            if (ownerContactFile != null) {
                ownerCompSName = ownerContactFile.getDescriptorValue("ContactShortName");
                ownerCompName = ownerContactFile.getDescriptorValue("ObjectName");
            }
            from = ownerCompName;
            receiver = mainCompName;
        }else{
            from = mainCompName;
        }
        log.info("from:" + from);
        log.info("receiver:" + receiver);

        IControl fieldSender = dialog.getFieldByName("ccmTrmtSender");
        if (fieldSender != null && fieldSender instanceof ITextField) {
            log.info("field by name:::" + fieldSender);
            log.info("from :::" + from);
            log.info("receiver :::" + receiver);
            ITextField textField = (ITextField) fieldSender;
            textField.setText(from);
        }

        IControl fieldReceiver = dialog.getFieldByName("ccmTrmtReceiver");
        if (fieldReceiver != null && fieldReceiver instanceof ITextField) {
            log.info("field by name:::" + fieldReceiver);
            log.info("from :::" + from);
            log.info("receiver :::" + receiver);
            ITextField textField = (ITextField) fieldReceiver;
            textField.setText(receiver);
        }

        List<IInformationObject> updateList = new ArrayList<>();
        IInformationObject parentObject = getTask().getProcessInstance().getMainInformationObject();

        if(parentObject == null){
            IInformationObjectLinks attachLink= getTask().getProcessInstance().getLoadedInformationObjectLinks();
            parentObject =  attachLink.getLinks().get(0).getTargetInformationObject();
        }

        if( parentObject != null){
            updateList.add(parentObject);
            IInformationObject prjCardDoc = GeneralLib.getProjectCard(getTask().getSession(), parentObject.getDescriptorValue("ccmPRJCard_code"));
            if(prjCardDoc!=null) updateList.add(prjCardDoc);

            for(IInformationObject sourceObje:updateList){

                Vector<IControl> fields = dlg.getFields();
                for(IControl ctrl : fields){
                   // if(!ctrl.isReadonly()) continue;

                    if (ctrl.getName()== null || ctrl.getName().isEmpty()) continue;

                    String descID = ctrl.getDescriptorId();
                    String parentVal = sourceObje.getDescriptorValue(descID);
                    if(parentVal == null) continue;
                    if(parentVal.isEmpty()) continue;

                    Utils.setText(dlg , ctrl.getName() , parentVal);


                }

            }

        }

        super.onInitMetadataDialog(dialog);
    }

    public void onInitMetadataDialogx(final IDialog dialog) throws EvitaWebException {
            /*
        if (dialog != null && isNew()) {

            IDocument projeDoc ;
            IDocument contactDoc = getContactRecord(ses.getUser().getLogin());
            if(contactDoc != null) {

            }

            IControl fieldByName = dialog.getFieldByName("ccmPRJCard_name");
            if (fieldByName != null && fieldByName instanceof ITextField) {
                ITextField textField = (ITextField) fieldByName;
                textField.setText("Yunus Emre Test");
            }
            fieldByName = dialog.getFieldByName("ccmPRJCard_code");
            if (fieldByName != null && fieldByName instanceof ITextField) {
                ITextField textField = (ITextField) fieldByName;
                textField.setText("KN12345");
            }

        }

             */

    }

}
