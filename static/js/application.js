var chat;

require({
    baseUrl: "js/jquery",
    paths: {
        jquery: "jquery-2.1.3",
        org: "../org"
    }
},
["jquery", "jquery.cometd"],
function ($, cometd) {
    $(document).ready(function () {
        var location = window.location.protocol + "//" + window.location.host;
        var user;


        cometd.init({
            url: location + "/channel/cometd",
            logLevel: 'info'
        });

        cometd.handshake();

        cometd.addListener('/meta/handshake', _metaHandshake);
        cometd.addListener('/meta/connect', _metaConnect);

        function _metaHandshake(handshake) {
            console.log(handshake);
            if (handshake.successful === true) {
                cometd.subscribe("/data/output", _messageHandler);
            }
        }

        function _metaConnect(message) {
            console.log(message);
        }

        function _messageHandler(message) {
            var response = message.data;
            console.log("received message: " + response);

            var command = response.command;
            if (command == "login") {
                user = $.parseJSON(response.user);
                $("#login").hide();
                $("#chatframe").show();
            } else {
                $("#chat").append(response);
            }
        }

        chat = {
            send: function(msg) {
                cometd.publish("/data/input", {
                    msg: msg
                });
            },

            login: function(username) {
                cometd.publish("/data/command", {
                    command: "login",
                    username: username
                })
            }
        };
    })
});
