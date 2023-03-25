from flask import Flask, jsonify, request
from flask_sockets import Sockets

import sqlite3
import base64
import datetime
from threading import Thread

import logging
logging.basicConfig(level=logging.INFO)
 


app = Flask(__name__)
sockets = Sockets(app)

db = "conversationalist.db"

message = ["", ""]

flag = [0];


webSocks = {}


"""def thread_func(ws, username):
    webSocks[username] = ws
    while not ws.closed:
        continue
    """

@sockets.route('/echo/<username>')
def echo(ws, username):
    print(username + ": connected")
    webSocks[username] = ws
    while not ws.closed:
        if ws.receive() == "ola":
            print("ola")
    #thread = Thread(target=thread_func, args=(ws, username))
    #thread.daemon = True
    #thread.start()
    #thread.join()
    
            

def create_connection(db_file):
    """ create a database connection to the SQLite database
        specified by db_file
    :param db_file: database file
    :return: Connection object or None
    """
    conn = None
    try:
        conn = sqlite3.connect(db_file)
        return conn
    except Exception as e:
        print(e)

    return conn



@app.route("/", methods=['GET'])
def server_up():
    print("server up")
    return jsonify({"server up": 1})

@app.route("/add_user/<username>", methods=['POST', 'GET'])
def add_user(username):
    if request.method == 'GET':
        con = create_connection(db)
        cur = con.cursor()
        cur.execute("SELECT username FROM users WHERE username == ?", (username,))
        rows = cur.fetchall()
        if len(rows) > 0:
            return jsonify({'user': 0})
        else:
            return jsonify({'user': 2})

    else:
        con = create_connection(db)
        cur = con.cursor()
        cur.execute("insert into users(username) values (?)", (username,))
        con.commit()
        return jsonify({'user': 1})


@app.route("/<username>/createConvo/<convoName>/<c_type>", methods=['POST'])
def create_convo(username, convoName, c_type):
    con = create_connection(db)
    cur = con.cursor()
    cur.execute("SELECT name FROM conversations WHERE name == ?", (convoName,))
    rows = cur.fetchall()
    if len(rows) > 0:
        return jsonify({'name_free': 0})
    else:
        cur.execute("INSERT INTO conversations (name, type) values (?,?)", (convoName, c_type))
        last_id = cur.lastrowid
        cur.execute("INSERT INTO relation_users_conv (username, conv_id, notify, read) values (?,?, false, 1)", (username, last_id))
        con.commit()
        return  jsonify({'name_free': 1})
    
    

@app.route("/store_geo_atts/<username>/<convoName>/<latitude>/<longitude>/<radius>", methods=['POST'])
def store_geo_atts(username, convoName, latitude, longitude, radius):
    con = create_connection(db)
    cur = con.cursor()
    cur.execute("SELECT name FROM conversations WHERE name == ?", (convoName,))
    rows = cur.fetchall()
    if len(rows) > 0:
        return jsonify({'name_free': 0})
    else:
        cur.execute("INSERT INTO geofences (name, latitude, longitude, radius) values (?,?,?,?)", (convoName, latitude, longitude, radius))
        cur.execute("INSERT INTO conversations (name, type) values (?, ?)", (convoName, "Geo-fenced"))
        last_id = cur.lastrowid
        cur.execute("INSERT INTO relation_users_conv (username, conv_id, notify, read) values (?,?,false,1)", (username, last_id))
        
        con.commit()
        return jsonify({'name_free': 1})
    


@app.route("/<username>/convos", methods=['GET'])
def show_user_convos(username):
    con = create_connection(db)
    cur = con.cursor()
    cur.execute("SELECT row_id, cg.name, type, read, latitude, longitude, radius FROM (conversations AS c LEFT JOIN geofences AS g ON c.name == g.name) AS cg JOIN relation_users_conv ON (row_id=conv_id) WHERE "
                "username == ?", (username,))
    rows = cur.fetchall()
    rows_dic_list = []
    if len(rows) == 0:
        return jsonify([{'no_chats': 1}])

    for row in rows:
        rows_dic = {"id": row[0], "name": row[1], "type": row[2], "read": row[3], "latitude": row[4], "longitude": row[5], "radius": row[6]}
        rows_dic_list.append(rows_dic)

    return jsonify(rows_dic_list)


@app.route("/convos/<convo_name>/<username>", methods=['POST'])
def join_user_chat(username, convo_name):
    con = create_connection(db)
    cur = con.cursor()
    cur.execute("SELECT row_id FROM conversations WHERE name == ?", (convo_name, ))
    rows = cur.fetchall()
    if len(rows) <= 0:
        return jsonify({'success': 0})
    else:
        conv_id = rows[0][0]
        cur.execute("SELECT conv_id FROM relation_users_conv WHERE username==? AND conv_id ==?", (username, conv_id))
        rows = cur.fetchall()
        if len(rows) > 0:
            return jsonify({'success': 2})

        cur.execute("INSERT INTO relation_users_conv (username, conv_id, notify, read) values (?,?, false, 0)", (username, conv_id))
        con.commit()
        return jsonify({'success': 1})


@app.route("/messages/<chatname>/<username>", methods=['GET', 'POST'])
def get_chat(chatname, username):
    texts = []
    con = create_connection(db)
    cur = con.cursor()
    if request.method == 'GET':
        cur.execute("SELECT * FROM messages WHERE conv_id == (SELECT row_id FROM conversations WHERE name == ?) ORDER BY time_stamp ASC", (chatname,))
        rows = cur.fetchall()
        for row in rows:
            text = { "sender": row[0], "content": row[3], "time": row[2], "id": row[5]}
            texts.append(text)
        cur.execute("UPDATE relation_users_conv SET read = 1, notify = false WHERE conv_id == (SELECT row_id FROM conversations WHERE name == ?) AND username == ?", (chatname, username))
        con.commit()
        
        
    else:
        cur.execute("SELECT is_photo, is_map, is_poll FROM messages WHERE conv_id == (SELECT row_id FROM conversations WHERE name == ?) ORDER BY time_stamp ASC", (chatname,))
        rows = cur.fetchall()
        for row in rows:
            text = {"is_photo": row[0], "is_map": row[1], "is_poll": row[2]}
            texts.append(text)
    texts = texts[-10:]
    return jsonify(texts)


@app.route("/text_sent/<content>/<username>/<chatName>", methods=['POST'])
def send_text(content, username, chatName):
    con = create_connection(db)
    cur = con.cursor()
    cur.execute("SELECT row_id FROM conversations WHERE name == ?", (chatName,))
    row_id = cur.fetchone()[0]
    date_time = str(datetime.datetime.now())
    date_time = date_time[:16]
    cur.execute("INSERT INTO messages (user, conv_id, time_stamp, content, is_photo, is_map, is_poll) values (?,?,?,?, FALSE, FALSE, FALSE)", (username, row_id, date_time, content))
    
    cur.execute("UPDATE relation_users_conv SET notify = true, read = 0 WHERE conv_id == ? AND username != ?", (row_id, username))
    

    text_1 = str(username)+ "//" + str(chatName) + "//" + str(date_time) + "//" + str(content)
    message[0] = text_1
    print("sent")
    cur.execute("SELECT username FROM relation_users_conv WHERE username != ? AND conv_id == ?", (username, row_id))
    rows = cur.fetchall()
    print(rows)
    for row in rows:
        inChat = 1
        chat = row[0] + "Chat"
        main = row[0] + "Main"
        notify = row[0] + "Notify"
        try:
            webSocks[chat].send(text_1)
        except:
            inChat = 0
            pass
        try:
            webSocks[main].send(text_1)
        except:
            pass
        print("username inChat = ", row[0], inChat, flush=True)
        if inChat == 0:
            try:
                webSocks[notify].send(text_1)
            except:
                pass
            
    con.commit()
    return (jsonify({'text_sent': content}))



@app.route("/send/location/<username>/<chatName>/<content>", methods=['POST'])
def send_location(content, username, chatName):
    con = create_connection(db)
    cur = con.cursor()
    cur.execute("SELECT row_id FROM conversations WHERE name == ?", (chatName,))
    row_id = cur.fetchone()[0]
    date_time = str(datetime.datetime.now())
    date_time = date_time[:16]
    cur.execute("INSERT INTO messages (user, conv_id, time_stamp, content, is_photo, is_map, is_poll) values (?,?,?,?, FALSE, TRUE, FALSE)", (username, row_id, date_time, content))
    
    cur.execute("UPDATE relation_users_conv SET notify = true, read = 0 WHERE conv_id == ? AND username != ?", (row_id, username))
    

    text_1 = str(username)+ "//" + str(chatName) + "//" + str(date_time) + "//" + str(content)
    message[0] = text_1
    print("sent")
    cur.execute("SELECT username FROM relation_users_conv WHERE username != ? AND conv_id == ?", (username, row_id))
    rows = cur.fetchall()
    print(rows)
    for row in rows:
        chat = row[0] + "Chat"
        main = row[0] + "Main"
        try:
            webSocks[chat].send(text_1)
        except:
            pass
        try:
            webSocks[main].send(text_1)
        except:
            pass
            
    con.commit()
    return (jsonify({'text_sent': content}))





@app.route("/send/poll/<username>/<chatName>/<content>", methods=['POST'])
def send_poll(content, username, chatName):
    con = create_connection(db)
    cur = con.cursor()
    cur.execute("SELECT row_id FROM conversations WHERE name == ?", (chatName,))
    row_id = cur.fetchone()[0]
    date_time = str(datetime.datetime.now())
    date_time = date_time[:16]
    cur.execute("INSERT INTO messages (user, conv_id, time_stamp, content, is_photo, is_map, is_poll) values (?,?,?,?, FALSE, FALSE, TRUE)", (username, row_id, date_time, content))
    
    cur.execute("UPDATE relation_users_conv SET notify = true, read = 0 WHERE conv_id == ? AND username != ?", (row_id, username))
    

    text_1 = str(username)+ "//" + str(chatName) + "//" + str(date_time) + "//" + str(content)
    message[0] = text_1
    print("sent")
    cur.execute("SELECT username FROM relation_users_conv WHERE username != ? AND conv_id == ?", (username, row_id))
    rows = cur.fetchall()
    print(rows)
    for row in rows:
        chat = row[0] + "Chat"
        main = row[0] + "Main"
        try:
            webSocks[chat].send(text_1)
        except:
            pass
        try:
            webSocks[main].send(text_1)
        except:
            pass
            
    con.commit()
    return (jsonify({'text_sent': content}))


    
@app.route("/convos/<username>", methods=['GET'])
def show_public_convos(username):
    con = create_connection(db)
    cur = con.cursor()
    cur.execute("SELECT cg.name, type, latitude, longitude, radius FROM (conversations c LEFT JOIN geofences g ON c.name == g.name) AS cg WHERE ((type == 'Public') OR (type == 'Geo-fenced')) AND cg.name NOT IN"
                " (SELECT name FROM conversations JOIN relation_users_conv ON row_id = conv_id WHERE username == ?)", (username, ))
    rows = cur.fetchall()
    rows_dic_list = []
    if len(rows) == 0:
        return jsonify([{'no_chats': 1}])

    for row in rows:
        if not row[2]:
            rows_dic = {"name": row[0], "type": row[1], "latitude": "none", "longitude": "none", "radius": "none"}
        else:
            rows_dic = {"name": row[0], "type": row[1], "latitude": row[2], "longitude": row[3], "radius": str(row[4])}
        rows_dic_list.append(rows_dic)

    return jsonify(rows_dic_list)


@app.route("/leave_chat/<chatName>/<username>", methods=['POST'])
def leave_chat(username, chatName):
    con = create_connection(db)
    cur = con.cursor()
    cur.execute("SELECT row_id FROM conversations WHERE name == ?", (chatName, ))
    row_id = cur.fetchone()[0]
    cur.execute("SELECT * FROM relation_users_conv WHERE username == ? AND conv_id == ?", (username, row_id))
    row = cur.fetchone()
    print(row)
    cur.execute("DELETE FROM relation_users_conv WHERE username == ? AND conv_id == ?", (username, row_id))
    con.commit()
    return jsonify({"success": 1})



@app.route("/image_sent/image/<username>/<chatName>", methods=['POST'])
def send_image( username, chatName):   


    base64EncodedStr = base64.b64encode(bytearray(request.data)).decode("ascii")
     
    con = create_connection(db)
    cur = con.cursor()
    cur.execute("SELECT row_id FROM conversations WHERE name == ?", (chatName,))
    row_id = cur.fetchone()[0]
    date_time = str(datetime.datetime.now())
    date_time = date_time[:16]
    cur.execute("INSERT INTO messages (user, conv_id, time_stamp, content, is_photo, is_map, is_poll) values (?,?,?,?, TRUE, FALSE, FALSE)", (username, row_id, date_time, base64EncodedStr))
    cur.execute("UPDATE relation_users_conv SET notify = true, read = 0 WHERE conv_id == ? AND username != ?", (row_id, username))
    con.commit()
    
    
    text_1 = str(username)+ "//" + str(chatName) 
    message[0] = text_1
    print("sent")
    cur.execute("SELECT username FROM relation_users_conv WHERE username != ? AND conv_id == ?", (username, row_id))
    rows = cur.fetchall()
    for row in rows:
        inChat = 1
        chat = row[0] + "Chat"
        main = row[0] + "Main"
        notify = row[0] + "Notify"
        try:
            webSocks[chat].send(text_1)
        except:
            inChat = 0
            pass
        try:
            webSocks[main].send(text_1)
        except:
            pass
        print("username inChat = ", row[0], inChat, flush=True)
        if inChat == 0:
            try:
                webSocks[notify].send(text_1)
            except:
                pass
            
    con.commit()
    return (jsonify({'text_sent': "ola"}))



@app.route("/<username>/notify", methods=['GET'])
def notify(username):
    con = create_connection(db)
    cur = con.cursor()
    cur.execute("SELECT cg.name, type, latitude, longitude, radius FROM (conversations AS c LEFT JOIN geofences AS g on c.name == g.name) AS cg JOIN relation_users_conv ON conv_id == row_id WHERE username == ? AND notify == true", (username, ))
    rows = cur.fetchall()
    rows_dic_list = []
    if len(rows) == 0:
        return jsonify([{'no_chats': 1}])

    for row in rows:
        if not row[2]:
            rows_dic = {"name": row[0], "type": row[1], "latitude": "none", "longitude": "none", "radius": "none"}
        else:
            rows_dic = {"name": row[0], "type": row[1], "latitude": row[2], "longitude": row[3], "radius": str(row[4])}
        rows_dic_list.append(rows_dic)

    cur.execute("UPDATE relation_users_conv SET notify = false WHERE  username == ?", (username, ))
    con.commit()
    print(rows_dic_list, flush=True)
    return jsonify(rows_dic_list)



@app.route("/get_image/<msg_id>", methods=['GET'])
def get_image(msg_id):
    con = create_connection(db)
    cur = con.cursor()
    cur.execute("SELECT content FROM messages WHERE msg_id == ?", (msg_id,))
    row = cur.fetchone()
    image = row[0]
    return jsonify({"image": image})



@app.route("/more/messages/<chatname>/<msg_id>", methods=['GET', 'POST'])
def get_more_chat(chatname, msg_id):
    texts = []
    con = create_connection(db)
    cur = con.cursor()
    if request.method == 'GET':
        cur.execute("SELECT * FROM messages WHERE conv_id == (SELECT row_id FROM conversations WHERE name == ?) AND msg_id < ? ORDER BY time_stamp ASC", (chatname, msg_id))
        rows = cur.fetchall()
        for row in rows:
            text = { "sender": row[0], "content": row[3], "time": row[2], "id": row[5]}
            texts.append(text)
        
        
    else:
        cur.execute("SELECT is_photo, is_map, is_poll FROM messages WHERE conv_id == (SELECT row_id FROM conversations WHERE name == ?) AND msg_id < ? ORDER BY time_stamp ASC", (chatname, msg_id))
        rows = cur.fetchall()
        for row in rows:
            text = {"is_photo": row[0], "is_map": row[1], "is_poll": row[2]}
            texts.append(text)
    texts = texts[-10:]
    return jsonify(texts)

@app.route("/messages/realtime/<username>/<chatname>/<msg_id>", methods=['GET', 'POST'])
def realtime_chat(username, chatname, msg_id):
    texts = []
    con = create_connection(db)
    cur = con.cursor()
    if request.method == 'GET':
        cur.execute(
            "SELECT * FROM messages WHERE conv_id == (SELECT row_id FROM conversations WHERE name == ?) AND msg_id > ? ORDER BY time_stamp ASC",
            (chatname, msg_id))
        rows = cur.fetchall()
        for row in rows:
            text = {"sender": row[0], "content": row[3], "time": row[2], "id": row[5]}
            texts.append(text)
        cur.execute("UPDATE relation_users_conv SET notify = false, read = 1 WHERE conv_id == (SELECT row_id FROM conversations WHERE name == ?) AND username == ?", (chatname, username))
        con.commit()


    else:
        cur.execute(
            "SELECT is_photo, is_map, is_poll FROM messages WHERE conv_id == (SELECT row_id FROM conversations WHERE name == ?) AND msg_id > ? ORDER BY time_stamp ASC",
            (chatname, msg_id))
        rows = cur.fetchall()
        if len(rows) == 0:
            response = [{"none": 1}]
            return jsonify(response)
        for row in rows:
            text = {"is_photo": row[0], "is_map": row[1], "is_poll":row[2]}
            texts.append(text)
    return jsonify(texts)



@app.route("/getCacheMessages/<chatname>", methods=['GET', 'POST'])
def get_cache_messages(chatname):
    texts = []
    con = create_connection(db)
    cur = con.cursor()
    if request.method == 'GET':
        cur.execute("SELECT * FROM messages WHERE conv_id == (SELECT row_id FROM conversations WHERE name == ?) ORDER BY time_stamp ASC", (chatname,))
        rows = cur.fetchall()
        for row in rows:
            text = { "sender": row[0], "content": row[3], "time": row[2], "id": row[5]}
            texts.append(text)
                
    else:
        cur.execute("SELECT is_photo, is_map, is_poll FROM messages WHERE conv_id == (SELECT row_id FROM conversations WHERE name == ?) ORDER BY time_stamp ASC", (chatname,))
        rows = cur.fetchall()
        for row in rows:
            text = {"is_photo": row[0], "is_map": row[1], "is_poll": row[2]}
            texts.append(text)
    texts = texts[-8:]
    return jsonify(texts)

@app.route("/tryThis/<content>", methods=['GET'])
def tryThis(content):
    message = content
    date_time = str(datetime.datetime.now())
    timeStamp = date_time[:16]
    user = "Fred"
    conv_id = 56
    con = create_connection(db)
    cur = con.cursor()
    cur.execute("INSERT INTO messages (user, conv_id, time_stamp, content, is_photo) values (?,?,?,?, FALSE)", (user, conv_id, timeStamp, message))
    text_1 = str(user)+ "//" + str("ttg") + "//" + str(timeStamp) + "//" + str(message)
    cur.execute("SELECT username FROM relation_users_conv WHERE username != ? AND conv_id == ?", (user, conv_id))
    rows = cur.fetchall()
    for row in rows:
        print(row[0])
        chat = row[0] + "Chat"
        main = row[0] + "Main"
        
        try:
            webSocks[chat].send(text_1)
        except:
            pass
        try:
            webSocks[main].send(text_1)
        except:
            pass
    return jsonify({"yes":user})
    
if __name__ == '__main__':
    from gevent import pywsgi
    from geventwebsocket.handler import WebSocketHandler
    
    print("server up at port 5000")
    server = pywsgi.WSGIServer(('', 5000), app, handler_class=WebSocketHandler)
    server.serve_forever()

