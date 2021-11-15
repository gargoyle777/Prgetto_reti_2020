# Prgetto_reti_2020
This was my reti's project.
Basically it's a competitive game to play 1v1 in a translation game, with login and ranking.
main connection tcp, and notification in udp (wasnt allowed to use call back for learning reason). the server use selector to manage the clients, the client is multithread to manage the udp connection. JSON used to log users data and an external API to provide the translated words for the challenge.
