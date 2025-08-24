package com.system.chattalkdesktop.common;


import com.system.chattalkdesktop.utils.NavigationUtil;
import com.system.chattalkdesktop.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;

public class sidebarController {

    @FXML
    public void navigateToProfile( MouseEvent event ) {
        NavigationUtil.switchScene(event,"/com/system/chattalkdesktop/ProfilePage/Profile.fxml","Profile");
    }
    @FXML
    public void navigateToChat( MouseEvent event ) {
//        NavigationUtil.switchScene(event,"/com/system/chattalkdesktop/MainChat/ChatApp.fxml","ChatApp❤");
    }
    @FXML
    public void navigateToGroup( MouseEvent event ) {
//        NavigationUtil.switchScene(event,"/com/system/chattalkdesktop/GroupPage/Group.fxml","Groups❤");
    }
    @FXML
    public void navigateToSearch( MouseEvent event ) {
        NavigationUtil.switchScene(event,"/com/system/chattalkdesktop/SearchPage/SearchForUsers.fxml","Find Friends 💌");

    }
    @FXML
    public void logout( MouseEvent event ) {
        SessionManager.getInstance().clearSession();
//        NavigationUtil.switchScene(event,"/com/system/chattalkdesktop/auth/Login.fxml","login🎶");
//   NotificationManager.getInstance().c
    }
}
