import argparse
import json
import requests


BASE_URL = "http://localhost"
BASE_PORT = 4080

URL = "{}:{}".format(BASE_URL, BASE_PORT)


def create_conversation():
    url = "{}/conversation".format(URL)
    response = requests.post(url)
    return response.json()['id']


def send_event(conversation_id):
    url = "{}/conversation/event".format(URL)
    payload = {"id": "someId",
               "conversationId": conversation_id,
               "type": "someType"}

    headers = {"Content-Type": "application/json"}

    response = requests.post(url,
                             data=json.dumps(payload),
                             headers=headers)
    status = response.status_code
    print "Sent event to %s. Response status [%s]" % (conversation_id, status)


def get_events_processed(conversation_id):
    url = "{}/conversation/{}/events".format(URL, conversation_id)

    response = requests.get(url)
    return response.json()['eventsProcessed']


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("-cid",
                        "--conversation_id",
                        help="target conversation id")

    args = parser.parse_args()

    cid = args.conversation_id

    if cid is None:
        cid = create_conversation()

    n_events_send = 10

    for _ in xrange(n_events_send):
        send_event(cid)

    events_processed = get_events_processed(cid)

    print "Conversation %s processed %s events." % (cid, events_processed)


if __name__ == "__main__":
    main()
