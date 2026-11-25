"""Cliente consumidor AMQP 1.0: lê UMA mensagem e encerra.

Cada instância tem um --name (C1..C5). Com --durable em um tópico, cria uma
assinatura durável (container-id + link name fixos e expiry-policy never):
o broker retém as mensagens enquanto o consumidor está offline.
"""
import argparse
from proton import Terminus
from proton.handlers import MessagingHandler
from proton.reactor import Container
from common import BROKER, log, conectar


class Consumer(MessagingHandler):
    def __init__(self, name, address, durable, timeout):
        super().__init__(auto_accept=True)
        self.name, self.address = name, address
        self.durable, self.timeout = durable, timeout
        self.got = False

    def on_start(self, event):
        conn = self.conn = conectar(event.container)
        opts = []
        if self.durable:
            from proton.reactor import DurableSubscription
            opts.append(DurableSubscription())
        event.container.create_receiver(conn, self.address, name=self.name, options=opts)
        log(self.name, f"conectado; aguardando 1 mensagem em {self.address} "
                       f"(durável={self.durable})")
        self.timer = event.container.schedule(self.timeout, self)

    def on_message(self, event):
        m = event.message
        log(self.name, f"RECEBEU '{m.body}' (durable={m.durable}, id={m.id})")
        self.got = True
        event.receiver.close()
        event.connection.close()
        self.timer.cancel()

    def on_connection_opened(self, event):
        log(self.name, "conexão AMQP aberta com o broker")

    def on_transport_error(self, event):
        log(self.name, f"ERRO de transporte: {event.transport.condition}")

    def on_connection_error(self, event):
        log(self.name, f"ERRO de conexão: {event.connection.remote_condition}")

    def on_link_error(self, event):
        log(self.name, f"ERRO no link: {event.link.remote_condition}")

    def on_timer_task(self, event):
        if not self.got:
            log(self.name, f"TIMEOUT {self.timeout}s sem mensagem")
            self.conn.close()


if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("--name", required=True)
    p.add_argument("--address", default="queue://demo")
    p.add_argument("--durable", action="store_true")
    p.add_argument("--timeout", type=float, default=400)
    a = p.parse_args()
    c = Container(Consumer(a.name, a.address, a.durable, a.timeout))
    c.container_id = a.name          # identidade estável (necessária p/ durável)
    c.run()
