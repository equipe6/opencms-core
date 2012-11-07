/*
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (C) Alkacon Software (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.ade.properties.client;

import org.opencms.ade.properties.shared.I_CmsAdePropertiesConstants;
import org.opencms.gwt.client.A_CmsEntryPoint;
import org.opencms.gwt.client.CmsCoreProvider;
import org.opencms.gwt.client.property.CmsPropertySubmitHandler;
import org.opencms.gwt.client.property.CmsSimplePropertyEditorHandler;
import org.opencms.gwt.client.property.CmsVfsModePropertyEditor;
import org.opencms.gwt.client.property.definition.CmsPropertyDefinitionDialog;
import org.opencms.gwt.client.property.definition.CmsPropertyDefinitionMessages;
import org.opencms.gwt.client.rpc.CmsRpcAction;
import org.opencms.gwt.client.ui.CmsPushButton;
import org.opencms.gwt.client.ui.I_CmsButton.ButtonStyle;
import org.opencms.gwt.client.ui.css.I_CmsLayoutBundle;
import org.opencms.gwt.client.ui.input.form.CmsDialogFormHandler;
import org.opencms.gwt.client.ui.input.form.CmsFormDialog;
import org.opencms.gwt.client.ui.input.form.I_CmsFormSubmitHandler;
import org.opencms.gwt.shared.CmsCoreData;
import org.opencms.gwt.shared.property.CmsPropertiesBean;
import org.opencms.util.CmsUUID;

import java.util.ArrayList;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PopupPanel;

/**
 * Entry point class for the standalone ADE properties dialog.<p>
 */
public class CmsPropertiesEntryPoint extends A_CmsEntryPoint {

    /** Flag which indicates that the property definition dialog needs to be opened. */
    protected boolean m_needsPropertyDefinitionDialog;

    /** The link to open after editing the properties / property definition is finished. */
    protected String m_closeLink;

    /**
     * @see org.opencms.gwt.client.A_CmsEntryPoint#onModuleLoad()
     */
    @Override
    public void onModuleLoad() {

        super.onModuleLoad();
        m_closeLink = CmsCoreProvider.getMetaElementContent(I_CmsAdePropertiesConstants.META_BACKLINK);
        String resource = CmsCoreProvider.getMetaElementContent(I_CmsAdePropertiesConstants.META_RESOURCE);
        editProperties(new CmsUUID(resource));
    }

    /**
     * Starts the property editor for the resource with the given structure id.<p>
     * 
     * @param structureId the structure id of a resource
     */
    protected void editProperties(final CmsUUID structureId) {

        CmsRpcAction<CmsPropertiesBean> action = new CmsRpcAction<CmsPropertiesBean>() {

            @Override
            public void execute() {

                start(0, true);
                CmsCoreProvider.getVfsService().loadPropertyData(structureId, this);
            }

            @Override
            protected void onResponse(CmsPropertiesBean result) {

                I_CmsLayoutBundle.INSTANCE.propertiesCss().ensureInjected();
                CmsSimplePropertyEditorHandler handler = new CmsSimplePropertyEditorHandler(null);
                handler.setPropertiesBean(result);
                CmsVfsModePropertyEditor editor = new CmsVfsModePropertyEditor(result.getPropertyDefinitions(), handler);
                editor.setReadOnly(result.isReadOnly());
                editor.setShowResourceProperties(!handler.isFolder());
                stop(false);
                final CmsFormDialog dialog = new CmsFormDialog(handler.getDialogTitle(), editor.getForm());

                CmsCoreData.UserInfo userInfo = CmsCoreProvider.get().getUserInfo();
                if (userInfo.isDeveloper()) {
                    String style = I_CmsLayoutBundle.INSTANCE.propertiesCss().propertyDefinitionButton();
                    CmsPushButton button = new CmsPushButton(style);
                    button.setTitle(CmsPropertyDefinitionMessages.messageDialogCaption());
                    button.setButtonStyle(ButtonStyle.TRANSPARENT, null);
                    button.getElement().getStyle().setFloat(Style.Float.LEFT);
                    dialog.addButton(button);
                    button.addClickHandler(new ClickHandler() {

                        public void onClick(ClickEvent event) {

                            m_needsPropertyDefinitionDialog = true;
                            dialog.hide();
                            editPropertyDefinition();
                        }
                    });
                }
                CmsDialogFormHandler formHandler = new CmsDialogFormHandler();
                formHandler.setDialog(dialog);
                I_CmsFormSubmitHandler submitHandler = new CmsPropertySubmitHandler(handler);
                formHandler.setSubmitHandler(submitHandler);
                editor.getForm().setFormHandler(formHandler);
                editor.initializeWidgets(dialog);
                dialog.centerHorizontally(50);
                dialog.addCloseHandler(new CloseHandler<PopupPanel>() {

                    public void onClose(CloseEvent<PopupPanel> event) {

                        onClosePropertyDialog();
                    }

                });
                dialog.catchNotifications();
            }
        };
        action.execute();
    }

    /**
     * Opens the dialog for creating new property definitions.<p>
     */
    protected void editPropertyDefinition() {

        CmsRpcAction<ArrayList<String>> action = new CmsRpcAction<ArrayList<String>>() {

            @Override
            public void execute() {

                start(200, true);
                CmsCoreProvider.getVfsService().getDefinedProperties(this);
            }

            @Override
            protected void onResponse(ArrayList<String> result) {

                stop(false);
                CmsPropertyDefinitionDialog dialog = new CmsPropertyDefinitionDialog(result);
                dialog.center();
                dialog.addCloseHandler(new CloseHandler<PopupPanel>() {

                    public void onClose(CloseEvent<PopupPanel> event) {

                        onClosePropertyDefinitionDialog();
                    }

                });
            }

        };
        action.execute();
    }

    /**
     * This method is called after the property definition dialog is closed.<p>
     */
    protected void onClosePropertyDefinitionDialog() {

        closeDelayed();
    }

    /**
     * This method is called after the property dialog is closed.<p>
     */
    protected void onClosePropertyDialog() {

        if (!m_needsPropertyDefinitionDialog) {
            closeDelayed();
        }
    }

    /**
     * Returns to the close link after a short delay.<p>
     */
    private void closeDelayed() {

        Timer timer = new Timer() {

            @Override
            public void run() {

                Window.Location.assign(m_closeLink);

            }
        };
        timer.schedule(300);
    }

}
