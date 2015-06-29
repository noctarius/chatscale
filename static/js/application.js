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
        cometd.init({
            url: "http://localhost:8080/channel/cometd",
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
            console.log("received message: " + message.data);
            $("#chat").append(message.data);
        }

        chat = {
            send: function(name, msg) {
                cometd.publish("/data/input", {
                    name: name,
                    msg: msg
                });
            }
        };
    })
});
