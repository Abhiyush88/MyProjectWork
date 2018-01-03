
Objective
==========

Designe a network application over HTTP 2.0 for streaming media from a local File server to a PC, with an Android device acting as a remote.


Project description:
---------------------
The three devices involved in this project are categorized as:
    a.Controller - The android device which would be used to play and control the media file
    b.Renderer - The Computer which plays the media stream
    c.Server - The Computer which contains the media files.


Protocols and Implementation: 
------------------------------
The application layer protocol we designed for this project is called VLC-Remote Protocol and it was only used between the Controller and Media Server. The other two connections used just existing application layer protocol.


Platform:
----------
Linux
Server/deploy_server.sh - start the server
Player/deploy_player.sh - start the renderer (VLC player must be installed)
Controller/remote/app/build/outputs/apk/app-debug.apk - Android app for controller
