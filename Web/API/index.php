<?php
//todo: aktuelle version nicht online (hostinger spinnt) (-> online altes inquire, sehr anders overall)
//var_dump($_POST);
date_default_timezone_set('UTC');
define("B64", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/");
$dev = true;
$local = false;
if (!isset($_SERVER['REMOTE_ADDR'])) {
    $dev = true;
    $local = true;
}
if ($dev) {
    if ($local) {
        $_POST['a'] = "REGISTER";
        $_POST['username'] = "onnoe";
        $_POST['password'] = "1234";
        $_POST['name'] = "Onno";
        $_POST['birthday'] = "19980218";
        $_POST['gender'] = "m";
//        $_POST['a'] = "GET";
//        $_POST['table'] = "users";
//        $_POST['id_key'] = "id";
//        $_POST['id_value'] = "12345678";
//        $_POST['column'] = "username";{
//        $_POST['a'] = "INUP";
//        $_POST['table'] = "users";
//        $_POST['key_0'] = "name";
//        $_POST['value_0'] = "hihi";
    } else if (isset($_GET['a']))
        $_POST = $_GET;
} else
    error_reporting(0);
if ($dev && $local)
    $db = new PDO("mysql:dbname=moti;host=localhost", "root", "1234");
else
    $db = new PDO("mysql:dbname=u601419936_moti;host=mysql.hostinger.de", "u601419936_admin", "hostingeR134652");
$fcms = array();
switch ($_POST['a']) {
    case ("EXEC") : {
        $db->query($_POST['sql']);
        break;
    }
    case ("GETMEETINGPOINTS") : {
        $mps = array();
        $rows = getall("meetingpoints", "status", "o");
        $bounds = explode(";", $_POST['bounds']);
        $ne = explode(",", $bounds[0]);
        $sw = explode(",", $bounds[1]);
        $min_lat = $sw[0];
        $max_lat = $ne[0];
        $min_lng = $sw[1];
        $max_lng = $ne[1];
        for ($i = 0; $i < count($rows); $i++) {
            $loc = explode(",", $rows[$i]["location"]);
            if (($loc[0] > $min_lat) && ($loc[0] < $max_lat) && ($loc[1] > $min_lng) && ($loc[1] < $max_lng))
                array_push($mps, $rows[$i]);
        }
        echo json_encode(array("mps" => $mps));
        break;
    }
    case ("REGISTER") : {
        $salt = bin2hex(openssl_random_pseudo_bytes(32));
        $pw = $_POST['password'];
        $hash = hash("sha256", $pw . $salt);
        $password = $hash . $salt;
        if (get("users", "username", $_POST['username']) == null) {
            inup(array(
                "table" => "users",
                "key_0" => "username",
                "value_0" => $_POST['username'],
                "key_1" => "password",
                "value_1" => $password,
                "key_2" => "name",
                "value_2" => $_POST['name'],
                "key_3" => "info",
                "value_3" => $_POST['birthday'] . "," . $_POST['gender']
            ));
            echo json_encode(array("id" => get("users", "username", $_POST['username'], "id")));
        } else
            echo json_encode(array("id" => "*FALSE*"));
        break;
    }
    case ("LOGIN") : {
        $response = array("result" => "*FALSE*", "notice" => "", "row" => array());
        $dbpw = get("users", "username", $_POST['username'], "password");
        if ($dbpw == null)
            $response['result'] = "*NULL*";
        else if (hash("sha256", $_POST['password'] . substr($dbpw, 64, 64)) == substr($dbpw, 0, 64)) {
            $response['result'] = "*OK*";
            if (strlen($dbpw) == 256) {
                inup(array(
                    "table" => "users",
                    "key_0" => "username",
                    "value_0" => $_POST['username'],
                    "key_1" => "password",
                    "value_1" => substr($dbpw, 0, 128)
                ));
            }
            $response['row'] = get("users", "username", $_POST['username']);
        } else if (strlen($dbpw) == 256 && hash("sha256", $_POST['password'] . substr($dbpw, 192, 64)) == substr($dbpw, 128, 64)) {
            $response['result'] = "*OK*";
            $response['notice'] = "RECOVERED";
            inup(array(
                "table" => "users",
                "key_0" => "username",
                "value_0" => $_POST['username'],
                "key_1" => "password",
                "value_1" => substr($dbpw, 128, 128)
            ));
            $response['row'] = get("users", "username", $_POST['username']);
        }
        echo json_encode($response);
        break;
    }
    case ("FORGOT_PASSWORD") : {
        $response = array("result" => "*FALSE*", "notice" => "");
        $user = get("users", "username", $_POST['email']);
        if ($user == null)
            $response['result'] = "*NULL*";
        else {
            $old_pw = get("users", "username", $_POST['email'], "password");
            if (strlen($old_pw) == 256)
                $response['notice'] = "ANEW";
            $npw = "";
            for ($i = 0; $i < 10; $i++)
                $npw .= B64[mt_rand(0, 61)];
            $salt = bin2hex(openssl_random_pseudo_bytes(32));
            $hash = hash("sha256", hash("sha256", $npw) . $salt);
            $newpassword = $hash . $salt;
            inup(array(
                "table" => "users",
                "key_0" => "username",
                "value_0" => $_POST['email'],
                "key_1" => "password",
                "value_1" => substr($old_pw, 0, 128) . $newpassword
            ));
            $response['result'] = mail($_POST['email'], "Password Recovery", "Your new Moti-password is:\n\n" . $npw, "From: Moti<info@moti.16mb.com>", "-f info@moti.16mb.com") ? "*OK*" : "*FALSE*";
        }
        echo json_encode($response);
        break;
    }
    case ("CHANGE_PASSWORD") : {
        $response = array("result" => "*FALSE*");
        $user = get("users", "id", $_POST['uid']);
        if ($user == null)
            $response['result'] = "*NULL*";
        else if (hash("sha256", $_POST['old_pw'] . substr($user['password'], 64, 64)) == substr($user['password'], 0, 64)) {
            $response['result'] = "*OK*";
            $salt = bin2hex(openssl_random_pseudo_bytes(32));
            $hash = hash("sha256", $_POST['new_pw'] . $salt);
            $password = $hash . $salt;
            inup(array(
                "table" => "users",
                "key_0" => "id",
                "value_0" => $_POST['uid'],
                "key_1" => "password",
                "value_1" => $password
            ));
        }
        echo json_encode($response);
        break;
    }
    case ("SEND_CHAT") : {
        sendFCM(array($_POST['to']), array(
            "message" => "CHAT",
            "text" => $_POST['message'],
            "uid" => $_POST['from']
        ));
        break;
    }
    case ("MAIL") : {
        $body = $_POST['body'];
        $subject = $_POST['subject'];
        $recipient = $_POST['recipient'];
        mail($recipient, $subject, $body, "From: Moti<noreply@moti.web44.net>", "-f noreply@moti.web44.net");
        break;
    }
    case ("GET") : {
        $values = array();
        if (isset($_POST["column"]))
            $values['value'] = get($_POST['table'], $_POST['id_key'], $_POST['id_value'], $_POST['column']);
        else
            $values['row'] = get($_POST['table'], $_POST['id_key'], $_POST['id_value']);
        echo json_encode($values);
        break;
    }
    case ("INUP") : {
        inup($_POST, true);
        break;
    }
}

foreach (array_keys($fcms) as $uid)
    foreach (array_keys($fcms[$uid]) as $message)
        sendFCM(array($uid), $fcms[$uid][$message]);

function get($table, $id_key, $id_value, $column = null)
{
    global $db;
    if ($id_key == "id" && strlen($id_value) == 9 && substr($id_value, 0, 1) == substr($table, 0, 1))
        $id_value = substr($id_value, 1);
    $sql = "SELECT * FROM `" . $table . "` WHERE `" . $id_key . "` = '" . $id_value . "'";
    $result = $db->query($sql);
    $rows = array();
    while ($row = $result->fetch())
        array_push($rows, $row);
    if ($column == null)
        return count($rows) > 0 ? $rows[0] : null;
    else
        return count($rows) > 0 ? $rows[0][$column] : null;
}

function getall($table, $id_key = null, $id_value = null, $column = null)
{
    global $db;
    if ($id_key != null && $id_key == "id" && strlen($id_value) == 9 && substr($id_value, 0, 1) == substr($table, 0, 1))
        $id_value = substr($id_value, 1);
    $sql = "SELECT * FROM `" . $table . "`";
    if ($id_key != null)
        $sql = "SELECT * FROM `" . $table . "` WHERE `" . $id_key . "` = '" . $id_value . "'";
    $result = $db->query($sql);
    $rows = array();
    while ($row = $result->fetch())
        array_push($rows, $row);
    $values = array();
    for ($i = 0; $i < count($rows); $i++)
        array_push($values, $rows[$i][$column]);
    if ($column == null)
        return $rows;
    else
        return $values;
}


function makeFCM($uids, $data)
{
    global $fcms;
    foreach ($uids as $uid) {
        if (!array_key_exists($uid, $fcms))
            $fcms[$uid][$data["message"]] = $data;
        else {
            if (!array_key_exists($data["message"], $fcms[$uid]))
                $fcms[$uid][$data["message"]] = $data;
            else
                foreach (array_keys($fcms[$uid][$data["message"]]) as $key)
                    $fcms[$uid][$data["message"]][$key] .= "," . $data[$key];
        }
    }
}

function sendFCM($uids, $data)
{
    $registration_ids = array();
    for ($i = 0; $i < count($uids); $i++) {
        $rid = get("users", "id", $uids[$i], "fcm_rid");
        if ($rid != null)
            array_push($registration_ids, $rid);
    }
    if (count($registration_ids) > 0) {
        $fields = array(
            'registration_ids' => $registration_ids,
            'data' => $data,
        );
        $url = 'https://fcm.googleapis.com/fcm/send';
        $headers = array(
            'Authorization: key=AIzaSyAJBMBi5j8MNiN8X9gWuiMKhql7Az5KDQE',
            'Content-Type: application/json'
        );
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($fields));
        curl_exec($ch);
        curl_close($ch);
    }
}

function inup($data, $inquire = false)
{
    global $db;
    $table = $data['table'];
    $pairs = array();
    for ($i = 0; $i < count($data) - 1; $i++) {
        if (isset($data['key_' . $i]) && $data['key_' . $i] != "") {
            $pairs[$i]["key"] = $data['key_' . $i];
            $pairs[$i]["value"] = $data['value_' . $i];
            if ($table == "users" && $pairs[$i]["key"] == "location" && $pairs[$i]["value"] != "") {
                $pairs[$i]["value"] .= "," . date("YmdHis");
            }
        }
    }
    if ($pairs[0]["key"] == "id" && strlen($pairs[0]["value"]) == 9 && substr($pairs[0]["value"], 0, 1) == substr($table, 0, 1))
        $pairs[0]["value"] = substr($pairs[0]["value"], 1);
    $columns = get($table, $pairs[0]["key"], $pairs[0]["value"]);
    if ($columns["id"] == null && (count($pairs) == 1 || $pairs[1]["key"] != "*DELETE*")) {
        $action = "i";
        $id = "";
        $o = 1;
        for ($i = 0; $i < $o; $i++) {
            for ($ii = 0; $ii < 8; $ii++)
                $id .= B64[mt_rand(0, 63)];
            if (get($table, "id", $id, "id") != null) {
                $id = "";
                $o++;
            }
        }
        $sql = "INSERT INTO `" . $table . "` (`id`, ";
        for ($i = 0; $i < count($pairs); $i++) {
            $sql .= "`" . $pairs[$i]["key"] . "`";
            if ($i < (count($pairs) - 1))
                $sql .= ", ";
        }
        $sql .= ")VALUES('" . $id . "', ";
        for ($i = 0; $i < count($pairs); $i++) {
            $sql .= "'" . $pairs[$i]["value"] . "'";
            if ($i < (count($pairs) - 1))
                $sql .= ", ";
        }
        $sql .= ")";
    } else if ($pairs[1]["key"] != "*DELETE*") {
        $action = "u";
        $sql = "UPDATE `" . $table . "` SET ";
        for ($i = 1; $i < count($pairs); $i++) {
            $sql .= "`" . $pairs[$i]["key"] . "` = '" . $pairs[$i]["value"] . "'";
            if ($i < (count($pairs) - 1))
                $sql .= ", ";
        }
        $sql .= " WHERE `" . $pairs[0]["key"] . "` = '" . $pairs[0]["value"] . "'";
    } else {
        $action = "d";
        $sql = "DELETE FROM `" . $table . "` WHERE `" . $pairs[0]["key"] . "` = '" . $pairs[0]["value"] . "'";
    }
    $db->query($sql);

    if ($inquire) {
        if ($action == "d") {
            if ($table == "users") {
                // gucken, was für rs in u.near standen -> r.uid benachrichtigen, uid in r.rejections eintragen
                foreach (explode(",", $columns["near"]) as $nid)
                    if (substr($nid, 0, 1) == "r") {
                        $request = get("requests", "id", $nid);
                        inup(array(
                            "table" => "requests",
                            "key_0" => "id",
                            "value_0" => $nid,
                            "key_1" => "rejections",
                            "value_1" => strlen($request["rejections"]) == 0 ? $columns["id"] : $request["rejections"] . "," . $columns["id"]
                        ));
                        makeFCM(array($request["uid"]), array(
                            "message" => "REQUEST_REJECTION",
                            "rid" => $nid,
                            "uid" => $columns["id"]
                        ));
                    }
            } else if ($table == "requests") {
                // gucken, welche users in r.notified stehen -> benachrichtigen dass request over ist & rid aus u.near entfernen.
                foreach (explode(",", $columns["notified"]) as $nid) {
                    $user = get("users", "id", $nid);
                    $near_new = array();
                    foreach (explode(",", $user["near"]) as $near)
                        if ($near != "r" . $columns["id"])
                            array_push($near_new, $near);
                    inup(array(
                        "table" => "users",
                        "key_0" => "id",
                        "value_0" => $nid,
                        "key_1" => "near",
                        "value_1" => implode(",", $near_new)
                    ));
                }
                makeFCM(explode(",", $columns["notified"]), array(
                    "message" => "REQUEST_OVER",
                    "rid" => $columns["id"]
                ));
            }
        } else {
            $columns = get($table, $pairs[0]["key"], $pairs[0]["value"]);
            for ($i = 0; $i < count($pairs); $i++) {
                if ($table == "users" && $pairs[$i]["key"] == "location") {
                    if ($pairs[$i]["value"] == "") {
                        // invisible.
                        // gucken, was für rs in user.near stehen -> p1s der rs benachrichtigen, "rejections" eintragen.
                        // u.near clearen
                        // aus ldb werden alle requests und mps automatisch gelöscht.
                        foreach (explode(",", $columns["near"]) as $nid)
                            if (substr($nid, 0, 1) == "r") {
                                $request = get("requests", "id", $nid);
                                inup(array(
                                    "table" => "requests",
                                    "key_0" => "id",
                                    "value_0" => $nid,
                                    "key_1" => "rejections",
                                    "value_1" => strlen($request["rejections"]) == 0 ? $columns["id"] : $request["rejections"] . "," . $columns["id"]
                                ));
                                makeFCM(array($request["uid"]), array(
                                    "message" => "REQUEST_REJECTION",
                                    "rid" => $nid,
                                    "uid" => $columns["id"]
                                ));
                            }
                        inup(array(
                            "table" => "users",
                            "key_0" => "id",
                            "value_0" => $columns["id"],
                            "key_1" => "near",
                            "value_1" => ""
                        ));
                    } else {
                        // location geändert -> u.inquire!
                        // mps(25km) und requests(r+25km) durchforsten,nach nähe suchen.
                        // alles was gefunden wird in (r.notified) Und u.near eintragen & user benachrichtigen (wenn nicht bereits in u.near)
                        // wenn mps in u.near sind, die nicht nah sind, aus u.near entfernen, user benachrichtigen.
                        foreach (getall("meetingpoints") as $mp) {
                            $ul = explode(",", $pairs[$i]["value"]);
                            $ml = explode(",", $mp["location"]);
                            $dist = acos(sin(deg2rad($ul[0])) * sin(deg2rad($ml[0])) + cos(deg2rad($ul[0])) * cos(deg2rad($ml[0])) * cos(deg2rad($ml[1]) - deg2rad($ul[1]))) * 6371000;
                            if ($dist < 30000 && !in_array("m" . $mp["id"], explode(",", $columns["near"]))) {
                                makeFCM(array($columns["id"]), array(
                                    "message" => "NEAR_MP",
                                    "id" => $mp["id"],
                                    "row" => $mp
                                ));
                                inup(array(
                                    "table" => "users",
                                    "key_0" => "id",
                                    "value_0" => $columns["id"],
                                    "key_1" => "near",
                                    "value_1" => strlen($columns["near"]) == 0 ? "m" . $mp["id"] : $columns["near"] . "," . "m" . $mp["id"]
                                ));
                            } else if ($dist > 30000 && in_array("m" . $mp["id"], explode(",", $columns["near"]))) {
                                $near_new = array();
                                foreach (explode(",", $columns["near"]) as $near)
                                    if ($near != "m" . $mp["id"])
                                        array_push($near_new, $near);
                                inup(array(
                                    "table" => "users",
                                    "key_0" => "id",
                                    "value_0" => $columns["id"],
                                    "key_1" => "near",
                                    "value_1" => implode(",", $near_new)
                                ));
                                makeFCM(array($columns["id"]), array(
                                    "message" => "GONE_MP",
                                    "id" => $mp["id"]
                                ));
                            }
                        }
                        foreach (getall("requests") as $r) {
                            $ul = explode(",", $pairs[$i]["value"]);
                            $rl = explode(",", $r["location"]);
                            $dist = acos(sin(deg2rad($ul[0])) * sin(deg2rad($rl[0])) + cos(deg2rad($ul[0])) * cos(deg2rad($rl[0])) * cos(deg2rad($rl[1]) - deg2rad($ul[1]))) * 6371000;
                            if ($dist < 30000 + $r["radius"] && !in_array("r" . $rl["id"], explode(",", $columns["near"]))) {
                                makeFCM(array($columns["id"]), array(
                                    "message" => "REQUEST",
                                    "id" => $r["id"],
                                    "row" => $r
                                ));
                                inup(array(
                                    "table" => "users",
                                    "key_0" => "id",
                                    "value_0" => $columns["id"],
                                    "key_1" => "near",
                                    "value_1" => strlen($columns["near"]) == 0 ? "r" . $r["id"] : $columns["near"] . "," . "r" . $r["id"]
                                ));
                                inup(array(
                                    "table" => "requests",
                                    "key_0" => "id",
                                    "value_0" => $r["id"],
                                    "key_1" => "notified",
                                    "value_1" => strlen($r["notified"]) == 0 ? $columns["id"] : $r["notified"] . "," . $columns["id"]
                                ));
                            }
                        }
                    }
                    break;
                } else if ($table == "requests" && $pairs[$i]["key"] == "location") {
                    // r.inquire! (neuer request)
                    // alle user durchsuchen, nach nähe (r+25km) suchen
                    // alles, was gefunden wird, eintragen in u.near, r.notified, user benachrichtigen
                    // todo: (auch in tt von nahen mps suchen, deren user benachrichtigen)
                    foreach (getall("users") as $u) {
                        $rl = explode(",", $pairs[$i]["value"]);
                        $ul = explode(",", $u["location"]);
                        $dist = acos(sin(deg2rad($rl[0])) * sin(deg2rad($ul[0])) + cos(deg2rad($rl[0])) * cos(deg2rad($ul[0])) * cos(deg2rad($ul[1]) - deg2rad($rl[1]))) * 6371000;
                        if ($dist < 30000 + $columns["radius"]) {
                            makeFCM(array($u["id"]), array(
                                "message" => "REQUEST",
                                "id" => $columns["id"],
                                "row" => $columns
                            ));
                            inup(array(
                                "table" => "users",
                                "key_0" => "id",
                                "value_0" => $u["id"],
                                "key_1" => "near",
                                "value_1" => strlen($u["near"]) == 0 ? "r" . $columns["id"] : $u["near"] . "," . "r" . $columns["id"]
                            ));
                            inup(array(
                                "table" => "requests",
                                "key_0" => "id",
                                "value_0" => $columns["id"],
                                "key_1" => "notified",
                                "value_1" => strlen($columns["notified"]) == 0 ? $u["id"] : $columns["notified"] . "," . $u["id"]
                            ));
                        }
                    }
                    break;
                } else if ($table == "meetingpoints" && $pairs[$i]["key"] == "status" && $pairs[$i]["value"] == "o") {
                    // m.inquire!
                    // alle user durchsuchen, nach nähe suchen
                    // alles, was gefunden wird, eintragen in u.near, user benachrichtigen
                    // (alle omps im 500m radius töten)
                    foreach (getall("users") as $u) {
                        $ml = explode(",", $columns["location"]);
                        $ul = explode(",", $u["location"]);
                        $dist = acos(sin(deg2rad($ml[0])) * sin(deg2rad($ul[0])) + cos(deg2rad($ml[0])) * cos(deg2rad($ul[0])) * cos(deg2rad($ul[1]) - deg2rad($ml[1]))) * 6371000;
                        if ($dist < 30000 && !in_array("m" . $columns["id"], explode(",", $u["near"]))) {
                            makeFCM(array($columns["id"]), array(
                                "message" => "NEW_MP",
                                "id" => $columns["id"],
                                "row" => $columns
                            ));
                            inup(array(
                                "table" => "users",
                                "key_0" => "id",
                                "value_0" => $u["id"],
                                "key_1" => "near",
                                "value_1" => strlen($u["near"]) == 0 ? "m" . $columns["id"] : $u["near"] . "," . "m" . $columns["id"]
                            ));
                        }
                    }
                    break;
                }
            }
        }
    }
    //todo: ich habe mp.near nicht benutzt. wofür soll das genau da sein? (ja wohl nicht nur für mp-opferung--das wäre unnötig)
    //todo: eventuell noch user.location->td mit einbeziehen?
}

//    // OLD INQUIRE
//    $inquire = false;
//    $location = "";
//    $radius = -1;
//    if ($table == "meetingpoints" || $table == "users" || $table == "requests") {
//        if ($action == "d")
//            $inquire = true;
//        for ($i = 0; $i < count($pairs); $i++) {
//            if ($pairs[$i]["name"] == "location" || $pairs[$i]["name"] == "radius"/* || $pairs[$i]["name"] == "gender" || $pairs[$i]["name"] == "birthday" || $pairs[$i]["name"] == "ages"*/) {
//                if ($pairs[$i]["name"] == "location" && $table != "request") {
//                    $location = $pairs[$i]["value"];
//                    if ($location == "")
//                        $action = "dl";
//                } else if ($pairs[$i]["name"] == "radius") {
//                    $radius = $pairs[$i]["value"];
//                    if ($radius == "")
//                        $radius = 500;
//                }
//                $inquire = true;
//            }
//        }
//    }
//    if ($inquire) {
//        if ($action != "d" && $action != "dl") {
//            if ($location == "") {
//                if ($table != "request")
//                    $location = get($table, $pairs[0]["name"], $pairs[0]["value"], "location");
//                else {
//                    $pre_l = get($table, $pairs[0]["name"], $pairs[0]["value"], "location");
//                    if ($pre_l == "u") {
//                        $location = get("users", "id", get($table, $pairs[0]["name"], $pairs[0]["value"], "uid"), "location");
//                    } else
//                        $location = get("meetingpoints", "id", $pre_l, "location");
//                }
//            }
//            if ($radius == -1) {
//                if ($table == "meetingpoints")
//                    $radius = 0;
//                else {
//                    $radius = get($table, $pairs[0]["name"], $pairs[0]["value"], "radius");
//                    if ($radius == "")
//                        $radius = 500;
//                }
//            }
//            $locarray = explode(",", $location);
//            $x1 = deg2rad($locarray[0]);
//            $y1 = deg2rad($locarray[1]);
//            $type = $table == "users" ? "u" : ($table == "requests" ? "r" : "m");
//            $uid = $type == "u" ? get($table, $pairs[0]["name"], $pairs[0]["value"], "id") : ($type == "r" ? get($table, $pairs[0]["name"], $pairs[0]["value"], "uid") : "-1");
//            $near_id = $type . get($table, $pairs[0]["name"], $pairs[0]["value"], "id");
//            $all_users = getall("users");
//            $my_nears = array();
//            if ($type == "u")
//                $my_nears = explode(";", get($table, $pairs[0]["name"], $pairs[0]["value"], "near"));
//            for ($i = 0; $i < count($all_users); $i++) {
//                $ulocation = $all_users[$i]["location"];
//                $ulocarray = explode(",", $ulocation);
//                $timediff = strtotime(DateTime::createFromFormat("YmdHis", date("YmdHis"))->format("Y-m-d H:i:s")) - (strtotime(DateTime::createFromFormat("YmdHis", $ulocarray[2])->format("Y-m-d H:i:s")));
//                $x2 = deg2rad($ulocarray[0]);
//                $y2 = deg2rad($ulocarray[1]);
//                $d = acos(sin($x1) * sin($x2) + cos($x1) * cos($x2) * cos($y2 - $y1)) * 6371000;
//                $r = $radius + $all_users[$i]["radius"];
//                $nears = explode(";", $all_users[$i]["near"]);
//                $other_near_id = "";
//                if ($type == "u")
//                    $other_near_id = "u" . $all_users[$i]["id"];
//                if ($d <= $r && $all_users[$i]["id"] != $uid && $timediff < 30 * 60) {
//                    makeFCM(array($all_users[$i]["id"]), array(
//                        "message" => "near",
//                        "near_id" => $near_id,
//                        "row" => get($table, $pairs[0]["name"], $pairs[0]["value"])
//                    ));
//                    if (!in_array($near_id, $nears))
//                        $db->query("UPDATE `users` SET `near` = '" . $all_users[$i]["near"] . $near_id . ";' WHERE `id` = '" . $all_users[$i]["id"] . "'");
//                    if ($type == "u") {
//                        makeFCM(array($uid), array(
//                            "message" => "near",
//                            "near_id" => $other_near_id,
//                            "row" => $all_users[$i]
//                        ));
//                        if (!in_array($other_near_id, $my_nears))
//                            $db->query("UPDATE `users` SET `near` = '" . get($table, $pairs[0]["name"], $pairs[0]["value"], "near") . $other_near_id . ";' WHERE `id` = '" . $uid . "'");
//                        $my_nears = explode(";", get($table, $pairs[0]["name"], $pairs[0]["value"], "near"));
//                    }
//                } else {
//                    if (in_array($near_id, $nears)) {
//                        makeFCM(array($all_users[$i]["id"]), array(
//                            "message" => "not_near",
//                            "near_id" => $near_id
//                        ));
//                        $new_near = "";
//                        for ($ii = 0; $ii < count($nears); $ii++)
//                            if ($nears[$ii] != $near_id && $nears[$ii] != "")
//                                $new_near .= $nears[$ii] . ";";
//                        $db->query("UPDATE `users` SET `near` = '" . $new_near . "' WHERE `id` = '" . $all_users[$i]["id"] . "'");
//                    }
//                    if ($type == "u" && in_array($other_near_id, $my_nears)) {
//                        makeFCM(array($uid), array(
//                            "message" => "not_near",
//                            "near_id" => $other_near_id
//                        ));
//                        $new_near = "";
//                        for ($ii = 0; $ii < count($my_nears); $ii++)
//                            if ($my_nears[$ii] != $other_near_id && $my_nears[$ii] != "")
//                                $new_near .= $my_nears[$ii] . ";";
//                        $db->query("UPDATE `users` SET `near` = '" . $new_near . "' WHERE `id` = '" . $uid . "'");
//                        $my_nears = explode(";", get($table, $pairs[0]["name"], $pairs[0]["value"], "near"));
//                    }
//                }
//            }
//            if ($type == "u") {
//                $all_requests = getall("requests");
//                $all_mps = getall("meetingpoints");
//                $my_nears = explode(";", get($table, $pairs[0]["name"], $pairs[0]["value"], "near"));
//                for ($i = 0; $i < count($all_requests); $i++) {
//                    $pre_l = $all_requests[$i]["location"];
//                    if ($pre_l == "u")
//                        $rlocation = get("users", "id", $all_requests[$i]["uid"], "location");
//                    else
//                        $rlocation = get("meetingpoints", "id", $pre_l, "location");
//                    $rlocarray = explode(",", $rlocation);
//                    $x2 = deg2rad($rlocarray[0]);
//                    $y2 = deg2rad($rlocarray[1]);
//                    $timediff = 0;
//                    if (isset($rlocarray[2]))
//                        $timediff = strtotime(DateTime::createFromFormat("YmdHis", date("YmdHis"))->format("Y-m-d H:i:s")) - (strtotime(DateTime::createFromFormat("YmdHis", $rlocarray[2])->format("Y-m-d H:i:s")));
//                    $d = acos(sin($x1) * sin($x2) + cos($x1) * cos($x2) * cos($y2 - $y1)) * 6371000;
//                    $r = $radius + $all_requests[$i]["radius"];
//                    $other_near_id = "r" . $all_requests[$i]["id"];
//                    if ($d <= $r && $timediff < 30 * 60) {
//                        makeFCM(array($uid), array(
//                            "message" => "near",
//                            "near_id" => $other_near_id,
//                            "row" => $all_requests[$i]
//                        ));
//                        if (!in_array($other_near_id, $my_nears))
//                            $db->query("UPDATE `users` SET `near` = '" . get($table, $pairs[0]["name"], $pairs[0]["value"], "near") . $other_near_id . ";' WHERE `id` = '" . $uid . "'");
//                        $my_nears = explode(";", get($table, $pairs[0]["name"], $pairs[0]["value"], "near"));
//                    } else if (in_array($other_near_id, $my_nears)) {
//                        makeFCM(array($uid), array(
//                            "message" => "not_near",
//                            "near_id" => $other_near_id
//                        ));
//                        $new_near = "";
//                        for ($ii = 0; $ii < count($my_nears); $ii++)
//                            if ($my_nears[$ii] != $other_near_id && $my_nears[$ii] != "")
//                                $new_near .= $my_nears[$ii] . ";";
//                        $db->query("UPDATE `users` SET `near` = '" . $new_near . "' WHERE `id` = '" . $uid . "'");
//                        $my_nears = explode(";", get($table, $pairs[0]["name"], $pairs[0]["value"], "near"));
//                    }
//                }
//                for ($i = 0; $i < count($all_mps); $i++) {
//                    $mlocation = $all_mps[$i]["location"];
//                    $mlocarray = explode(",", $mlocation);
//                    $x2 = deg2rad($mlocarray[0]);
//                    $y2 = deg2rad($mlocarray[1]);
//                    $d = acos(sin($x1) * sin($x2) + cos($x1) * cos($x2) * cos($y2 - $y1)) * 6371000;
//                    $r = $radius;
//                    $other_near_id = "m" . $all_mps[$i]["id"];
//                    if ($d <= $r) {
//                        makeFCM(array($uid), array(
//                            "message" => "near",
//                            "near_id" => $other_near_id,
//                            "row" => $all_mps[$i]
//                        ));
//                        if (!in_array($other_near_id, $my_nears))
//                            $db->query("UPDATE `users` SET `near` = '" . get($table, $pairs[0]["name"], $pairs[0]["value"], "near") . $other_near_id . ";' WHERE `id` = '" . $uid . "'");
//                        $my_nears = explode(";", get($table, $pairs[0]["name"], $pairs[0]["value"], "near"));
//                    } else if (in_array($other_near_id, $my_nears)) {
//                        makeFCM(array($uid), array(
//                            "message" => "not_near",
//                            "near_id" => $other_near_id
//                        ));
//                        $new_near = "";
//                        for ($ii = 0; $ii < count($my_nears); $ii++)
//                            if ($my_nears[$ii] != $other_near_id && $my_nears[$ii] != "")
//                                $new_near .= $my_nears[$ii] . ";";
//                        $db->query("UPDATE `users` SET `near` = '" . $new_near . "' WHERE `id` = '" . $uid . "'");
//                        $my_nears = explode(";", get($table, $pairs[0]["name"], $pairs[0]["value"], "near"));
//                    }
//                }
//            }
//        } else {
//            $type = $table == "users" ? "u" : ($table == "requests" ? "r" : "m");
//            $near_id = $type . $columns["id"];
//            $all_users = getall("users");
//            $uid = "";
//            $my_nears = array();
//            if ($type == "u") {
//                $uid = get($table, $pairs[0]["name"], $pairs[0]["value"], "id");
//                $my_nears = explode(";", get($table, $pairs[0]["name"], $pairs[0]["value"], "near"));
//            }
//            for ($i = 0; $i < count($all_users); $i++) {
//                $other_near_id = "";
//                if ($type == "u")
//                    $other_near_id = "u" . $all_users[$i]["id"];
//                $nears = explode(";", $all_users[$i]["near"]);
//                if (in_array($near_id, $nears)) {
//                    makeFCM(array($all_users[$i]["id"]), array(
//                        "message" => "not_near",
//                        "near_id" => $near_id
//                    ));
//                    $new_near = "";
//                    for ($ii = 0; $ii < count($nears); $ii++)
//                        if ($nears[$ii] != $near_id && $nears[$ii] != "")
//                            $new_near .= $nears[$ii] . ";";
//                    $db->query("UPDATE `users` SET `near` = '" . $new_near . "' WHERE `id` = '" . $all_users[$i]["id"] . "'");
//                }
//                if ($type == "u" && in_array($other_near_id, $my_nears)) {
//                    makeFCM(array($uid), array(
//                        "message" => "not_near",
//                        "near_id" => $other_near_id
//                    ));
//                    $new_near = "";
//                    for ($ii = 0; $ii < count($my_nears); $ii++)
//                        if ($my_nears[$ii] != $other_near_id && $my_nears[$ii] != "")
//                            $new_near .= $my_nears[$ii] . ";";
//                    $db->query("UPDATE `users` SET `near` = '" . $new_near . "' WHERE `id` = '" . $uid . "'");
//                    $my_nears = explode(";", get($table, $pairs[0]["name"], $pairs[0]["value"], "near"));
//                }
//            }
//            if ($type == "u") {
//                $all_requests = getall("requests");
//                $all_mps = getall("meetingpoints");
//                for ($i = 0; $i < count($all_requests); $i++) {
//                    $other_near_id = "r" . $all_requests[$i]["id"];
//                    if (in_array($other_near_id, $my_nears)) {
//                        makeFCM(array($uid), array(
//                            "message" => "not_near",
//                            "near_id" => $other_near_id
//                        ));
//                        $new_near = "";
//                        for ($ii = 0; $ii < count($my_nears); $ii++)
//                            if ($my_nears[$ii] != $other_near_id && $my_nears[$ii] != "")
//                                $new_near .= $my_nears[$ii] . ";";
//                        $db->query("UPDATE `users` SET `near` = '" . $new_near . "' WHERE `id` = '" . $uid . "'");
//                        $my_nears = explode(";", get($table, $pairs[0]["name"], $pairs[0]["value"], "near"));
//                    }
//                }
//                for ($i = 0; $i < count($all_mps); $i++) {
//                    $other_near_id = "m" . $all_mps[$i]["id"];
//                    if (in_array($other_near_id, $my_nears)) {
//                        makeFCM(array($uid), array(
//                            "message" => "not_near",
//                            "near_id" => $other_near_id
//                        ));
//                        $new_near = "";
//                        for ($ii = 0; $ii < count($my_nears); $ii++)
//                            if ($my_nears[$ii] != $other_near_id && $my_nears[$ii] != "")
//                                $new_near .= $my_nears[$ii] . ";";
//                        $db->query("UPDATE `users` SET `near` = '" . $new_near . "' WHERE `id` = '" . $uid . "'");
//                        $my_nears = explode(";", get($table, $pairs[0]["name"], $pairs[0]["value"], "near"));
//                    }
//                }
//            }
//        }
//    }
//
//
//if (isset($_POST['a']) && $_POST['a'] != '') {
//    $a = $_POST['a'];
//    //mysql_connect('localhost', 'actio', '1234'); the roots hehe (bis auf topic ^^)
//    if ($dev && $local) {
//        mysql_connect('localhost', 'root', '1234');
//        mysql_select_db("moti");
//    } else {
//        mysql_connect('fdb6.runhosting.com', '2002608_moti', 'ahosT134652');
//        mysql_select_db("2002608_moti");
//    }
//    $response = array();
//    if ($a == 'EXEC') {
//        $db->query($_POST['sql']);
//        /*$sql = $_POST['sql'];
//        $result = $db->query($sql);
//        if ($result)
//            $response["success"] = 1;
//        else
//            $response["error"] = 1;
//        $response["sql"] = $sql;
//        echo json_encode($response);*/
//    } /*else if ($a == 'EXPLODE') {
//        $string = $_POST['string'];
//        $result = $db->query($sql);
//        $response["values"] = explode("{{;}}", $string);
//        $keys = array();
//        $values = array();
//        for ($i = 0; $i < count($response["values"]); $i++) {
//            $o = explode("{{:}}", $response["values"][$i]);
//            array_push($keys, $o[0]);
//            $values[$i] = array();
//            for ($ii = 0; $ii < count($o); $ii++) {
//                if (strpos($o[$ii], "{{,}}") !== false) {
//                    $oii = explode("{{,}}", $o[$ii]);
//                    $nextLevel = false;
//                    for ($iii = 0; $iii < count($oii); $iii++) {
//                        if (strpos($oii[$iii], "{{.}}") !== false) {
//                            $nextLevel = true;
//                            $oiii = explode("{{.}}", $oii[$iii]);
//                            $nextLevel2 = false;
//                            for ($iv = 0; $iv < count($oiii); $iv++) {
//                                if (strpos($oiii[$iv], "{{|}}") !== false) {
//                                    $nextLevel2 = true;
//                                    $oiv = explode("{{|}}", $oiii[$iv]);
//                                    $values[$i][$oii[0]][$oiii[0]][$oiv[0]] = $oiv;
//                                } else
//                                    $values[$i][$oii[0]][$oiii[0]][$iv] = $oiii[$iv];
//                            }
//                        } else
//                            $values[$i][$oii[0]][$iii] = $oii[$iii];
//                    }
//                } else {
//                    array_push($values[$i], $o[$ii]);
//                }
//            }
//        }
//        $response["values"] = array_combine($keys, $values);
//        $response["success"] = 1;
//        echo json_encode($response);
//    } else if ($a == 'INQUIRE') {
//        //$messages = get("users", "id", $_POST['uid'], "messages");
//        $sql = "SELECT * FROM `users` WHERE `id` = '" . $_POST['uid'] . "'";
//        $result = $db->query($sql);
//        $rows = array();
//        while ($row = $result->fetch())
//            array_push($rows, $row);
//        $messages = $rows[0]['messages'];
//        $response['messages'] = $messages;
//        if (!isset($_POST['location'])) {
//            $sql = "UPDATE `users` SET `location` = '' WHERE `id` = '" . $_POST['uid'] . "'";
//            $db->query($sql);
//        } else {
//            $loc = explode(",", $_POST['location']);
//            $sql = "UPDATE `users` SET `location` = '" . $_POST['location'] . "' WHERE `id` = '" . $_POST['uid'] . "'";
//            $db->query($sql);
//            $x1 = deg2rad($loc[0]);
//            $y1 = deg2rad($loc[1]);
//            $p1radius = $rows[0]['radius'];
//            $rids = array();
//            $sql = "SELECT * FROM `requests`";
//            $result = $db->query($sql);
//            $rows = array();
//            while ($row = $result->fetch())
//                array_push($rows, $row);
//            for ($i = 0; $i < count($rows); $i++) {
//                if ($rows[$i]['location'] == "u") {
//                    $sql = "SELECT * FROM `users` WHERE `id` = '" . $rows[$i]['uid'] . "'";
//                    $result = $db->query($sql);
//                    $_rows = array();
//                    while ($_row = $result->fetch())
//                        array_push($_rows, $_row);
//                    $rloc = explode(",", $_rows[0]['location']);
//                } else {
//                    //$rloc = explode(",", $rows[$i]['location']); -- wrong b/c it's either "u" or <mpid>, not latlng! (here it's <mpid>)
//                    $sql = "SELECT * FROM `meetingpoints` WHERE `id` = '" . $rows[$i]['location'] . "'";
//                    $result = $db->query($sql);
//                    $_rows = array();
//                    while ($_row = $result->fetch())
//                        array_push($_rows, $_row);
//                    $rloc = explode(",", $_rows[0]['location']);
//                }
//                $x2 = deg2rad($rloc[0]);
//                $y2 = deg2rad($rloc[1]);
//                $p2radius = $rows[$i]['radius'];
//                $r = $p1radius + $p2radius;
//                $d = acos(sin($x1) * sin($x2) + cos($x1) * cos($x2) * cos($y2 - $y1)) * 6371000;
//                if ($d <= $r) {
//                    $age_min = 0;
//                    $age_max = 0;
//                    if ($rows[$i]['ages'] != "") {
//                        $ages = explode(";", $rows[$i]['ages']);
//                        $age_min = $ages[0];
//                        $age_max = $ages[1];
//                    }
//                    if (((isset($_POST['age']) && $_POST['age'] != "" && $_POST['age'] >= $age_min && $_POST['age'] <= $age_max) || $rows[$i]['ages'] == "") && ($rows[$i]['gender'] == "a" || !isset($_POST['gender']) || $rows[$i]['gender'] == $_POST['gender']) && $rows[$i]['uid'] != $_POST['uid']) {
//                        array_push($rids, $rows[$i]['id']);
//                    }
//                }
//                //$response['d'] = $d;
//            }
//            $response['requests'] = $rids;
//            $pids = array();
//            $sql = "SELECT * FROM `users` WHERE `location` <> ''";
//            $result = $db->query($sql);
//            $rows = array();
//            while ($row = $result->fetch())
//                array_push($rows, $row);
//            for ($i = 0; $i < count($rows); $i++) {
//                $rloc = explode(",", $rows[$i]['location']);
//                $x2 = deg2rad($rloc[0]);
//                $y2 = deg2rad($rloc[1]);
//                $p2radius = $rows[$i]['radius'];
//                $r = $p1radius + $p2radius;
//                $d = acos(sin($x1) * sin($x2) + cos($x1) * cos($x2) * cos($y2 - $y1)) * 6371000;
//                if ($d <= $r && $rows[$i]['id'] != $_POST['uid']) {
//                    array_push($pids, $rows[$i]['id']);
//                }
//            }
//            $response['nearpeople'] = $pids;
//        }
//        $response["success"] = 1;
//        echo json_encode($response);
//    }*/ else if ($a == 'GETMEETINGPOINTS') {
//        $mps = array();
//        /*$sql = "SELECT * FROM `meetingpoints` WHERE `active` = '1'";
//        $result = $db->query($sql);
//        $rows = array();
//        while ($row = $result->fetch())
//            array_push($rows, $row);*/
//        $rows = getall("meetingpoints", "active", "1");
//        $bounds = explode(";", $_POST['bounds']);
//        $ne = explode(",", $bounds[0]);
//        $sw = explode(",", $bounds[1]);
//        $min_lat = $sw[0];
//        $max_lat = $ne[0];
//        $min_lng = $sw[1];
//        $max_lng = $ne[1];
//        for ($i = 0; $i < count($rows); $i++) {
//            $loc = explode(",", $rows[$i]["location"]);
//            if (($loc[0] > $min_lat) && ($loc[0] < $max_lat) && ($loc[1] > $min_lng) && ($loc[1] < $max_lng))
//                array_push($mps, $rows[$i]);
//        }
//        $response["mps"] = $mps;
//        /*if($result)
//            $response["success"] = 1;
//        else
//            $response["error"] = 1;*/
//        echo json_encode($response);
//    } else if ($a == 'MAIL') {
//        $body = $_POST['body'];
//        $subject = $_POST['subject'];
//        $recipient = $_POST['recipient'];
//        mail($recipient, $subject, $body, "From: Moti<noreply@moti.web44.net>", "-f noreply@moti.web44.net");
//        /*$result = mail($recipient, $subject, $body, "From: Moti<noreply@moti.web44.net>", "-f noreply@moti.web44.net");
//        if ($result)
//            $response["success"] = 1;
//        else
//            $response["error"] = 1;*/
//        echo json_encode($response);
//    } else if ($a == 'GET') {
//        if (isset($_POST["column"]))
//            $response['value'] = get($_POST['table'], $_POST['id_key'], $_POST['id_value'], $_POST['column']);
//        else
//            $response['row'] = get($_POST['table'], $_POST['id_key'], $_POST['id_value']);
//        echo json_encode($response);
//        //echo json_encode(get($_POST['table'], $_POST['id_key'], $_POST['id_value'], $_POST['column']));
//        /*$table = $_POST['table'];
//        $id_key = $_POST['id_key'];
//        $id_value = $_POST['id_value'];
//        $column = $_POST['column'];
//        $sql = "SELECT * FROM `" . $table . "` WHERE `" . $id_key . "` = '" . $id_value . "'";
//        $result = $db->query($sql);
//        $rows = array();
//        while ($row = $result->fetch())
//            array_push($rows, $row);
//        $columns = $rows[0];
//        if ($columns[$column] == null) {
//            $response["error"] = 1;
//        } else {
//            $response["success"] = 1;
//        }
//        $response["name"] = $column;
//        $response["value"] = $columns[$column];
//        $response["values"] = explode("{{;}}", $columns[$column]);
//        $keys = array();
//        $values = array();
//        for ($i = 0; $i < count($response["values"]); $i++) {
//            $o = explode("{{:}}", $response["values"][$i]);
//            array_push($keys, $o[0]);
//            $values[$i] = array();
//            for ($ii = 0; $ii < count($o); $ii++) {
//                if (strpos($o[$ii], "{{,}}") !== false) {
//                    $oii = explode("{{,}}", $o[$ii]);
//                    $nextLevel = false;
//                    for ($iii = 0; $iii < count($oii); $iii++) {
//                        if (strpos($oii[$iii], "{{.}}") !== false) {
//                            $nextLevel = true;
//                            $oiii = explode("{{.}}", $oii[$iii]);
//                            $nextLevel2 = false;
//                            for ($iv = 0; $iv < count($oiii); $iv++) {
//                                if (strpos($oiii[$iv], "{{|}}") !== false) {
//                                    $nextLevel2 = true;
//                                    $oiv = explode("{{|}}", $oiii[$iv]);
//                                    $values[$i][$oii[0]][$oiii[0]][$oiv[0]] = $oiv;
//                                } else
//                                    $values[$i][$oii[0]][$oiii[0]][$iv] = $oiii[$iv];
//                            }
//                        } else
//                            $values[$i][$oii[0]][$iii] = $oii[$iii];
//                    }
//                } else {
//                    array_push($values[$i], $o[$ii]);
//                }
//            }
//        }
//        $response["values"] = array_combine($keys, $values);
//        $response["row"] = $columns;
//        echo json_encode($response);*/
//    }/* else if ($a == 'GETALL') {
//        $table = $_POST['table'];
//        $column = $_POST['column'];
//        $id_key = $_POST['id_key'];
//        $id_value = $_POST['id_value'];
//
//        $sql = "SELECT * FROM `" . $table . "`";
//        if ($id_key != "")
//            $sql = "SELECT * FROM `" . $table . "` WHERE `" . $id_key . "` = '" . $id_value . "'";
//        $result = $db->query($sql);
//        $rows = array();
//        while ($row = $result->fetch())
//            array_push($rows, $row);
//        $value = array();
//        for ($i = 0; $i < count($rows); $i++)
//            $__values[$i] = $rows[$i][$column];
//        if ($rows == null || count($rows) == 0) {
//            $response["error"] = 1;
//        } else {
//            $response["success"] = 1;
//        }
//        $response["rows"] = $rows;
//        $response["value"] = $__values;
//        $_values = array();
//        for ($oo = 0; $oo < count($__values); $oo++) {
//            $_value = $__values[$oo];
//            $value = explode("{{;}}", $_value);
//            $keys = array();
//            $values = array();
//            for ($i = 0; $i < count($value); $i++) {
//                $o = explode("{{:}}", $value[$i]);
//                array_push($keys, $o[0]);
//                $values[$i] = array();
//                for ($ii = 0; $ii < count($o); $ii++) {
//                    if (strpos($o[$ii], "{{,}}") !== false) {
//                        $oii = explode("{{,}}", $o[$ii]);
//                        $nextLevel = false;
//                        for ($iii = 0; $iii < count($oii); $iii++) {
//                            if (strpos($oii[$iii], "{{.}}") !== false) {
//                                $nextLevel = true;
//                                $oiii = explode("{{.}}", $oii[$iii]);
//                                $nextLevel2 = false;
//                                for ($iv = 0; $iv < count($oiii); $iv++) {
//                                    if (strpos($oiii[$iv], "{{|}}") !== false) {
//                                        $nextLevel2 = true;
//                                        $oiv = explode("{{|}}", $oiii[$iv]);
//                                        $values[$i][$oii[0]][$oiii[0]][$oiv[0]] = $oiv;
//                                    } else
//                                        $values[$i][$oii[0]][$oiii[0]][$iv] = $oiii[$iv];
//                                }
//                            } else
//                                $values[$i][$oii[0]][$iii] = $oii[$iii];
//                        }
//                    } else {
//                        array_push($values[$i], $o[$ii]);
//                    }
//                }
//            }
//            $_values[$oo] = array_combine($keys, $values);
//        }
//        $response["values"] = $_values;
//        echo json_encode($response);
//    }*/ else if ($a == 'INUP') {
//        $table = $_POST['table'];
////        $doit = true;
//        $pairs = array();
//        for ($i = 0; $i < count($_POST) - 1; $i++) {
//            if (isset($_POST['key_' . $i]) && $_POST['key_' . $i] != "") {
//                $pairs[$i]["name"] = $_POST['key_' . $i];
//                $pairs[$i]["value"] = $_POST['value_' . $i];
//                if ($table == "users" && $pairs[$i]["name"] == "location" && $pairs[$i]["value"] != "") {
//                    $pairs[$i]["value"] .= "," . date("YmdHis");
//                }
//            }
////            if ($_POST['key_' . $i] == "ABORT" || $_POST['value_' . $i] == "ABORT")
////                $doit = false;
//        }
//        /*$sql = "SELECT * FROM `" . $table . "` WHERE `" . $pairs[0]["name"] . "` = '" . $pairs[0]["value"] . "'";
//        $result = $db->query($sql);
//        $rows = array();
//        while ($row = $result->fetch())
//            array_push($rows, $row);
//        $columns = $rows[0];*/
//        $columns = get($table, $pairs[0]["name"], $pairs[0]["value"]);
//        $action = "";
//        $sql = "";
//        if ($columns["id"] == null && $pairs[1]["name"] != "*DELETE*") {
//            $action = "i";
////            $response["action"] = "insert";
//            $sql = "INSERT INTO `" . $table . "` (";
//            for ($i = 0; $i < count($pairs); $i++) {
//                $sql .= "`" . $pairs[$i]["name"] . "`";
//                if ($i < (count($pairs) - 1))
//                    $sql .= ", ";
//            }
//            $sql .= ")VALUES(";
//            for ($i = 0; $i < count($pairs); $i++) {
//                $sql .= "'" . $pairs[$i]["value"] . "'";
//                if ($i < (count($pairs) - 1))
//                    $sql .= ", ";
//            }
//            $sql .= ")";
//        } else if ($pairs[1]["name"] != "*DELETE*") {
//            $action = "u";
////            $response["action"] = "update";
//            $sql = "UPDATE `" . $table . "` SET ";
//            for ($i = 1; $i < count($pairs); $i++) {
//                $sql .= "`" . $pairs[$i]["name"] . "` = '" . $pairs[$i]["value"] . "'";
//                if ($i < (count($pairs) - 1))
//                    $sql .= ", ";
//            }
//            $sql .= " WHERE `" . $pairs[0]["name"] . "` = '" . $pairs[0]["value"] . "'";
//        } else {
//            $action = "d";
////            $response["action"] = "delete";
//            $sql = "DELETE FROM `" . $table . "` WHERE `" . $pairs[0]["name"] . "` = '" . $pairs[0]["value"] . "'";
//        }
////        $result = false;
////        if ($doit)
//        /*$result = */
//        $db->query($sql);
////        if ($result !== false)
////            $response["success"] = 1;
////        else
////            $response["error"] = 1;
////        $response["sql1"] = $sql;
//        /*$sql = "SELECT * FROM `" . $table . "` WHERE `" . $pairs[0]["name"] . "` = '" . $pairs[0]["value"] . "'";
//        $result = $db->query($sql);
//        $rows = array();
//        while ($row = $result->fetch())
//            array_push($rows, $row);
//        $columns = $rows[0];
//        $response["row"] = $columns;
//        echo json_encode($response);*/
//
//        //inquiring
//        $inquire = false;
//        if ($table == "meetingpoints" || $table == "users" || $table == "requests") {
//            if ($action == "d")
//                $inquire = true;
//            $location = "";
//            $radius = -1;
//            for ($i = 0; $i < count($pairs); $i++) {
//                if ($pairs[$i]["name"] == "location" || $pairs[$i]["name"] == "radius" || $pairs[$i]["name"] == "gender" || $pairs[$i]["name"] == "birthday" || $pairs[$i]["name"] == "ages") {
//                    if ($pairs[$i]["name"] == "location" && $table != "request") {
//                        $location = $pairs[$i]["value"];
//                        if ($location == "")
//                            $action = "dl";
//                    } else if ($pairs[$i]["name"] == "radius")
//                        $radius = $pairs[$i]["value"];
//                    $inquire = true;
//                }
//            }
//        }
//        if ($inquire) {
//            if ($action != "d" && $action != "dl") {
//                if ($location == "") {
//                    if ($table != "request")
//                        $location = get($table, $pairs[0]["name"], $pairs[0]["value"], "location");
//                    else {
//                        $pre_l = get($table, $pairs[0]["name"], $pairs[0]["value"], "location");
//                        if ($pre_l == "u") {
//                            $location = get("users", "id", get($table, $pairs[0]["name"], $pairs[0]["value"], "uid"), "location");
//                        } else
//                            $location = get("meetingpoints", "id", $pre_l, "location");
//                    }
//                }
//                if ($radius == -1) {
//                    if ($table == "meetingpoints")
//                        $radius = 0;
//                    else {
//                        $radius = get($table, $pairs[0]["name"], $pairs[0]["value"], "radius");
//                        if ($radius = "")
//                            $radius = 500;
//                    }
//                }
//                $locarray = explode(",", $location);
//                $x1 = deg2rad($locarray[0]);
//                $y1 = deg2rad($locarray[1]);
//                $type = $table == "users" ? "u" : ($table == "requests" ? "r" : "m");
//                $uid = $type == "u" ? get($table, $pairs[0]["name"], $pairs[0]["value"], "id") : ($type == "r" ? get($table, $pairs[0]["name"], $pairs[0]["value"], "uid") : "-1");
//                $near_id = $type . get($table, $pairs[0]["name"], $pairs[0]["value"], "id");
//                $all_users = getall("users");
//                for ($i = 0; $i < count($all_users); $i++) {
//                    $ulocation = $all_users[$i]["location"];
//                    $ulocarray = explode(",", $ulocation);
//                    $timediff = strtotime(DateTime::createFromFormat("YmdHis", date("YmdHis"))->format("Y-m-d H:i:s")) - (strtotime(DateTime::createFromFormat("YmdHis", $ulocarray[2])->format("Y-m-d H:i:s")));
//                    $x2 = deg2rad($ulocarray[0]);
//                    $y2 = deg2rad($ulocarray[1]);
//                    $d = acos(sin($x1) * sin($x2) + cos($x1) * cos($x2) * cos($y2 - $y1)) * 6371000;
//                    $r = $radius + $all_users[$i]["radius"];
//                    $nears = explode(";", $all_users[$i]["near"]);
//                    if ($type == "u") {
//                        $other_near_id = "u" . $all_users[$i]["id"];
//                        $my_nears = explode(";", get($table, $pairs[0]["name"], $pairs[0]["value"], "near"));
//                    }
//                    if ($d <= $r && $all_users[$i]["id"] != $uid && $timediff < 30 * 60) {
//                        sendFCM(array($all_users[$i]["id"]), array(
//                            "message" => "near",
//                            "near_id" => $near_id,
//                            "row" => get($table, $pairs[0]["name"], $pairs[0]["value"])
//                        ));
//                        if (!in_array($near_id, $nears))
//                            $db->query("UPDATE `users` SET `near` = '" . $all_users[$i]["near"] . $near_id . ";' WHERE `id` = '" . $all_users[$i]["id"] . "'");
//                        if ($type == "u") {
//                            sendFCM(array($uid), array(
//                                "message" => "near",
//                                "near_id" => $other_near_id,
//                                "row" => $all_users[$i]
//                            ));
//                            if (!in_array($other_near_id, $my_nears))
//                                $db->query("UPDATE `users` SET `near` = '" . get($table, $pairs[0]["name"], $pairs[0]["value"], "near") . $other_near_id . ";' WHERE `id` = '" . $uid . "'");
//                        }
//                    } else {
//                        if (in_array($near_id, $nears)) {
//                            sendFCM(array($all_users[$i]["id"]), array(
//                                "message" => "not_near",
//                                "near_id" => $near_id
//                            ));
//                            $new_near = "";
//                            for ($ii = 0; $ii < count($nears); $ii++)
//                                if ($nears[$ii] != $near_id && $nears[$ii] != "")
//                                    $new_near .= $nears[$ii] . ";";
//                            $db->query("UPDATE `users` SET `near` = '" . $new_near . "' WHERE `id` = '" . $all_users[$i]["id"] . "'");
//                        }
//                        if ($type == "u" && in_array($other_near_id, $my_nears)) {
//                            sendFCM(array($uid), array(
//                                "message" => "not_near",
//                                "near_id" => $other_near_id
//                            ));
//                            $new_near = "";
//                            for ($ii = 0; $ii < count($my_nears); $ii++)
//                                if ($my_nears[$ii] != $other_near_id && $my_nears[$ii] != "")
//                                    $new_near .= $my_nears[$ii] . ";";
//                            $db->query("UPDATE `users` SET `near` = '" . $new_near . "' WHERE `id` = '" . $uid . "'");
//                        }
//                    }
//                }
//                if ($type == "u") {
//                    $all_requests = getall("requests");
//                    $all_mps = getall("meetingpoints");
//                    $my_nears = explode(";", get($table, $pairs[0]["name"], $pairs[0]["value"], "near"));
//                    for ($i = 0; $i < count($all_requests); $i++) {
//                        $pre_l = $all_requests[$i]["location"];
//                        if ($pre_l == "u")
//                            $rlocation = get("users", "id", $all_requests[$i]["uid"], "location");
//                        else
//                            $rlocation = get("meetingpoints", "id", $pre_l, "location");
//                        $rlocarray = explode(",", $rlocation);
//                        $x2 = deg2rad($rlocarray[0]);
//                        $y2 = deg2rad($rlocarray[1]);
//                        $timediff = 0;
//                        if (isset($rlocarray[2]))
//                            $timediff = strtotime(DateTime::createFromFormat("YmdHis", date("YmdHis"))->format("Y-m-d H:i:s")) - (strtotime(DateTime::createFromFormat("YmdHis", $rlocarray[2])->format("Y-m-d H:i:s")));
//                        $d = acos(sin($x1) * sin($x2) + cos($x1) * cos($x2) * cos($y2 - $y1)) * 6371000;
//                        $r = $radius + $all_requests[$i]["radius"];
//                        $other_near_id = "r" . $all_requests[$i]["id"];
//                        if ($d <= $r && $timediff < 30 * 60) {
//                            sendFCM(array($uid), array(
//                                "message" => "near",
//                                "near_id" => $other_near_id,
//                                "row" => $all_requests[$i]
//                            ));
//                            if (!in_array($other_near_id, $my_nears))
//                                $db->query("UPDATE `users` SET `near` = '" . get($table, $pairs[0]["name"], $pairs[0]["value"], "near") . $other_near_id . ";' WHERE `id` = '" . $uid . "'");
//                        } else if (in_array($other_near_id, $my_nears)) {
//                            sendFCM(array($uid), array(
//                                "message" => "not_near",
//                                "near_id" => $other_near_id
//                            ));
//                            $new_near = "";
//                            for ($ii = 0; $ii < count($my_nears); $ii++)
//                                if ($my_nears[$ii] != $other_near_id && $my_nears[$ii] != "")
//                                    $new_near .= $my_nears[$ii] . ";";
//                            $db->query("UPDATE `users` SET `near` = '" . $new_near . "' WHERE `id` = '" . $uid . "'");
//                        }
//                    }
//                    for ($i = 0; $i < count($all_mps); $i++) {
//                        $mlocation = $all_mps[$i]["location"];
//                        $mlocarray = explode(",", $mlocation);
//                        $x2 = deg2rad($mlocarray[0]);
//                        $y2 = deg2rad($mlocarray[1]);
//                        $d = acos(sin($x1) * sin($x2) + cos($x1) * cos($x2) * cos($y2 - $y1)) * 6371000;
//                        $r = $radius;
//                        $other_near_id = "m" . $all_mps[$i]["id"];
//                        if ($d <= $r) {
//                            sendFCM(array($uid), array(
//                                "message" => "near",
//                                "near_id" => $other_near_id,
//                                "row" => $all_mps[$i]
//                            ));
//                            if (!in_array($other_near_id, $my_nears))
//                                $db->query("UPDATE `users` SET `near` = '" . get($table, $pairs[0]["name"], $pairs[0]["value"], "near") . $other_near_id . ";' WHERE `id` = '" . $uid . "'");
//                        } else if (in_array($other_near_id, $my_nears)) {
//                            sendFCM(array($uid), array(
//                                "message" => "not_near",
//                                "near_id" => $other_near_id
//                            ));
//                            $new_near = "";
//                            for ($ii = 0; $ii < count($my_nears); $ii++)
//                                if ($my_nears[$ii] != $other_near_id && $my_nears[$ii] != "")
//                                    $new_near .= $my_nears[$ii] . ";";
//                            $db->query("UPDATE `users` SET `near` = '" . $new_near . "' WHERE `id` = '" . $uid . "'");
//                        }
//                    }
//                }
//            } else {
//                $type = $table == "users" ? "u" : ($table == "requests" ? "r" : "m");
//                $near_id = $type . $columns["id"];
//                $all_users = getall("users");
//                if ($type == "u") {
//                    $uid = get($table, $pairs[0]["name"], $pairs[0]["value"], "id");
//                    $my_nears = explode(";", get($table, $pairs[0]["name"], $pairs[0]["value"], "near"));
//                }
//                for ($i = 0; $i < count($all_users); $i++) {
//                    if ($type == "u")
//                        $other_near_id = "u" . $all_users[$i]["id"];
//                    $nears = explode(";", $all_users[$i]["near"]);
//                    if (in_array($near_id, $nears)) {
//                        sendFCM(array($all_users[$i]["id"]), array(
//                            "message" => "not_near",
//                            "near_id" => $near_id
//                        ));
//                        $new_near = "";
//                        for ($ii = 0; $ii < count($nears); $ii++)
//                            if ($nears[$ii] != $near_id && $nears[$ii] != "")
//                                $new_near .= $nears[$ii] . ";";
//                        $db->query("UPDATE `users` SET `near` = '" . $new_near . "' WHERE `id` = '" . $all_users[$i]["id"] . "'");
//                    }
//                    if ($type == "u" && in_array($other_near_id, $my_nears)) {
//                        sendFCM(array($uid), array(
//                            "message" => "not_near",
//                            "near_id" => $other_near_id
//                        ));
//                        $new_near = "";
//                        for ($ii = 0; $ii < count($my_nears); $ii++)
//                            if ($my_nears[$ii] != $other_near_id && $my_nears[$ii] != "")
//                                $new_near .= $my_nears[$ii] . ";";
//                        $db->query("UPDATE `users` SET `near` = '" . $new_near . "' WHERE `id` = '" . $uid . "'");
//                    }
//                }
//            }
//        }
//    }/* else if ($a == 'INUP_DATA') {
//        $table = $_POST['table'];
//        $id_key = $_POST['id_key'];
//        $id_value = $_POST['id_value'];
//        $column = $_POST['column'];
//        $level1 = $_POST['level1'];
//        $level2 = $_POST['level2'];
//        $level3 = $_POST['level3'];
//        $level4 = $_POST['level4'];
//        $value = $_POST['value'];
//        $sql = "SELECT * FROM `" . $table . "` WHERE `" . $id_key . "` = '" . $id_value . "'";
//        $result = $db->query($sql);
//        $rows = array();
//        while ($row = $result->fetch())
//            array_push($rows, $row);
//        $columns = $rows[0];
//        $data = explode("{{;}}", $columns[$column]);
//        $keys = array();
//        $values = array();
//        for ($i = 0; $i < count($data); $i++) {
//            $o = explode("{{:}}", $data[$i]);
//            array_push($keys, $o[0]);
//            $values[$i] = array();
//            for ($ii = 0; $ii < count($o); $ii++) {
//                if (strpos($o[$ii], "{{,}}") !== false) {
//                    $oii = explode("{{,}}", $o[$ii]);
//                    $nextLevel = false;
//                    for ($iii = 0; $iii < count($oii); $iii++) {
//                        if (strpos($oii[$iii], "{{.}}") !== false) {
//                            $nextLevel = true;
//                            $oiii = explode("{{.}}", $oii[$iii]);
//                            $nextLevel2 = false;
//                            for ($iv = 0; $iv < count($oiii); $iv++) {
//                                if (strpos($oiii[$iv], "{{|}}") !== false) {
//                                    $nextLevel2 = true;
//                                    $oiv = explode("{{|}}", $oiii[$iv]);
//                                    $values[$i][$oii[0]][$oiii[0]][$oiv[0]] = $oiv;
//                                } else
//                                    $values[$i][$oii[0]][$oiii[0]][$iv] = $oiii[$iv];
//                            }
//                        } else
//                            $values[$i][$oii[0]][$iii] = $oii[$iii];
//                    }
//                } else {
//                    array_push($values[$i], $o[$ii]);
//                }
//            }
//        }
//        $data = array_combine($keys, $values);
//        $levels = 4 - (($level1 == "") ? 1 : 0) - (($level2 == "") ? 1 : 0) - (($level3 == "") ? 1 : 0) - (($level4 == "") ? 1 : 0);
//        if ($levels == 4)
//            $data[$level1][$level2][$level3][$level4] = $value;
//        if ($levels == 3)
//            $data[$level1][$level2][$level3] = $value;
//        if ($levels == 2)
//            $data[$level1][$level2] = $value;
//        if ($levels == 1)
//            $data[$level1] = $value;
//        if ($levels == 0)
//            $data = $value;
//        $keysI = array_keys($data);
//        for ($i = 0; $i < count($data); $i++) {
//            if (is_array($data[$keysI[$i]])) {
//                $keysII = array_keys($data[$keysI[$i]]);
//                for ($ii = 0; $ii < count($data[$keysI[$i]]); $ii++) {
//                    if (is_array($data[$keysI[$i]][$keysII[$ii]])) {
//                        $keysIII = array_keys($data[$keysI[$i]][$keysII[$ii]]);
//                        for ($iii = 0; $iii < count($data[$keysI[$i]][$keysII[$ii]]); $iii++) {
//                            if (is_array($data[$keysI[$i]][$keysII[$ii]][$keysIII[$iii]])) {
//                                $keysIV = array_keys($data[$keysI[$i]][$keysII[$ii]][$keysIII[$iii]]);
//                                for ($iv = 0; $iv < count($data[$keysI[$i]][$keysII[$ii]][$keysIII[$iii]]); $iv++) {
//                                    if (is_array($data[$keysI[$i]][$keysII[$ii]][$keysIII[$iii]][$keysIV[$iv]])) {
//                                        $data[$keysI[$i]][$keysII[$ii]][$keysIII[$iii]][$keysIV[$iv]] =
//                                            implode("{{|}}", $data[$keysI[$i]][$keysII[$ii]][$keysIII[$iii]][$keysIV[$iv]]);
//                                    }
//                                }
//                                $data[$keysI[$i]][$keysII[$ii]][$keysIII[$iii]] = implode("{{.}}", $data[$keysI[$i]][$keysII[$ii]][$keysIII[$iii]]);
//                            }
//                        }
//                        $data[$keysI[$i]][$keysII[$ii]] = implode("{{,}}", $data[$keysI[$i]][$keysII[$ii]]);
//                    }
//                }
//                $data[$keysI[$i]] = implode("{{:}}", $data[$keysI[$i]]);
//            }
//        }
//        $data = implode("{{;}}", array_filter($data));
//        $sql = "UPDATE `" . $table . "` SET `" . $column . "` = '" . $data . "' WHERE `" . $id_key . "` = '" . $id_value . "'";
//        $result = $db->query($sql);
//        if ($result !== false)
//            $response["success"] = 1;
//        else
//            $response["error"] = 1;
//        $response["sql1"] = $sql;
//        $sql = "SELECT * FROM `" . $table . "` WHERE `" . $id_key . "` = '" . $id_value . "'";
//        $result = $db->query($sql);
//        $rows = array();
//        while ($row = $result->fetch())
//            array_push($rows, $row);
//        $columns = $rows[0];
//        $response["row"] = $columns;
//        echo json_encode($response);
//    }*/ else {
//        echo "Invalid Request";
//    }
//} else {
//    echo "Access Denied";
//}
//
//function get($table, $id_key, $id_value, $column = null)
//{
//    $sql = "SELECT * FROM `" . $table . "` WHERE `" . $id_key . "` = '" . $id_value . "'";
//    $result = $db->query($sql);
//    $rows = array();
//    while ($row = $result->fetch())
//        array_push($rows, $row);
//    if ($column == null)
//        return $rows[0];
//    else
//        return $rows[0][$column];
//}
//
//function getall($table, $id_key = null, $id_value = null, $column = null)
//{
//    $sql = "SELECT * FROM `" . $table . "`";
//    if ($id_key != null)
//        $sql = "SELECT * FROM `" . $table . "` WHERE `" . $id_key . "` = '" . $id_value . "'";
//    $result = $db->query($sql);
//    $rows = array();
//    while ($row = $result->fetch())
//        array_push($rows, $row);
//    $values = array();
//    for ($i = 0; $i < count($rows); $i++)
//        array_push($values, $rows[$i][$column]);
//    if ($column == null)
//        return $rows;
//    else
//        return $values;
//}
//
////function getall($table, $column, $id_key, $id_value) {
////    $sql = "SELECT * FROM `" . $table . "`";
////    if ($id_key != null)
////        $sql = "SELECT * FROM `" . $table . "` WHERE `" . $id_key . "` = '" . $id_value . "'";
////    $result = $db->query($sql);
////    $rows = array();
////    while ($row = $result->fetch())
////        array_push($rows, $row);
////    $value = array();
////    for ($i = 0; $i < count($rows); $i++)
////        $__values[$i] = $rows[$i][$column];
////    if ($rows == null || count($rows) == 0) {
////        $response["error"] = 1;
////    } else {
////        $response["success"] = 1;
////    }
////    $response["rows"] = $rows;
////    $response["value"] = $__values;
////}
//
//function sendFCM($uids, $data)
//{
//    $registration_ids = array();
//    for ($i = 0; $i < count($uids); $i++) {
//        /*$sql = "SELECT * FROM `users` WHERE `id` = '" . $uids[$i] . "'";
//        $result = $db->query($sql);
//        $rows = array();
//        while ($row = $result->fetch())
//            array_push($rows, $row);
//        $rid = $rows[0]["fcm_rid"];*/
//        $rid = get("users", "id", $uids[$i], "fcm_rid");
//        if ($rid != null)
//            array_push($registration_ids, $rid);
//    }
//    if (count($registration_ids) > 0) {
////    $registration_ids = array("eWMBsNnyVpA:APA91bH6ZsZKDGDtKb8x1M19C0xKXJX0t_xPJ-BlES6I9M4aGpErVtmLD3cypx7WjqXC5js78VSgjt_pLGXIDcNQkOxQdbok5PAKTUrA8s5Pckji95V9O0FKsegZYSE9s6RBFLECEHuW");
////    $data = array("message" => "yeah!", "type" => "m");
//        $fields = array(
//            'registration_ids' => $registration_ids,
////    'to' => "/topics/m",
//            'data' => $data,
//        );
//        $url = 'https://fcm-http.googleapis.com/fcm/send';
//        $headers = array(
//            'Authorization: key=AIzaSyAw_MQhtK_2ca9ce-fTeugYMxiguvwkqNo',
//            'Content-Type: application/json'
//        );
//        $ch = curl_init();
//        curl_setopt($ch, CURLOPT_URL, $url);
//        curl_setopt($ch, CURLOPT_POST, true);
//        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
//        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
//        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($fields));
//        curl_exec($ch);
//        curl_close($ch);
////    $result = curl_exec($ch);
////    if ($result === FALSE) {
////        die('Curl failed: ' . curl_error($ch));
////    }
////    echo $result;
//    }
//}
//
// i mean, i know, but, you know, it's a lot of shit work.. (also, so close!) (no edge!)
//function get($table, $id_key, $id_value, $column) {
//        $sql = "SELECT * FROM `".$table."` WHERE `".$id_key."` = '".$id_value."'";
//        $result = $db->query($sql);
//        $rows = array();
//        while($row = $result->fetch())
//            array_push($rows, $row);
//        $columns = $rows[0];
//        if($columns[$column] == null) {
//            $response["error"] = 1;
//        } else {
//            $response["success"] = 1;
//        }
//        $response["name"] = $column;
//        $response["value"] = $columns[$column];
//        //einführen: {{A}}
//        //password{{:}}1747b1f98f4a057b8bae03497894c670{{;}}name{{:}}Onno{{;}}created_dt{{:}}20151127160420{{:}}byme:D{{;}}messages{{:}}{{A}}{{,}}2{{,}}hallo{{,}}123456789{{:}}{{A}}{{,}}7{{,}}hallovon7{{,}}987654321{{;}}{{A}}{{:}}hallo{{:}}{{A}}{{,}}q{{,}}w{{:}}mhm{{;}}{{A}}{{:}}ha2{{,}}a2{{:}}ha3{{,}}a3
//        if(strpos($o[$ii], "{{;}}")) {
//            $response["values"] = explode("{{;}}", $columns[$column]);
//            $values = array();
//            //6 -> a1{{:}}{{A}}{{,}}v1{{,}}{{A}}{{.}}v21{{.}}v22{{:}}b1{{:}}{{A}}{{,}}n1{{,}}n2{{,}}{{A}}{{.}}n31{{.}}n32
//            for($i = 0; $i < count($response["values"]); $i++) {
//                $o = explode("{{:}}", $response["values"][$i]);
//                //{{A}}{{,}}n1{{,}}n2{{,}}{{A}}{{.}}n31{{.}}n32
//                $key = $o[0];
//                if($o[0] == "{{A}}")
//                    $key = $i;
//                $values[$key] = array();
//                for($ii = 1; $ii < count($o); $ii++) {
//                    if(strpos($o[$ii], "{{,}}")) {
//                        $oii = explode("{{,}}", $o[$ii]);
//                        //{{A}} | n1 | n2 | {{A}}{{.}}n31{{.}}n32
//                        $keyii = $oii[0];
//                        if($oii[0] == "{{A}}")
//                            $keyii = $ii;
//                        for($iii = 1; $iii < count($oii); $iii++) {
//                            if(strpos($oii[$iii], "{{.}}")) {
//                                $oiii = explode("{{.}}", $oii[$iii]);
//                                $keyiii = $oiii[0];
//                                if($oiii[0] == "{{A}}")
//                                    $keyiii = $iii;
//                                for($iv = 1; $iv < count($oiii); $iv++) {
//                                    if(strpos($oiii[$iv], "{{|}}")) {
//                                        $oiv = explode("{{|}}", $oiii[$iv]);
//                                        $values[$o[0]][$oii[0]][$oiii[0]][$oiv[0]] = $oiv;
//                                    } else if ($iv == count($oiii) - 1) {
//                                        if(count($oiii) == 2)
//                                            $values[$key][$keyii][$keyiii] = $oiii[1];
//                                        else {
//                                            //$values[$key][$keyii] = array();
//                                            $oiii2 = $oiii;
//                                            array_shift($oiii2);
//                                            array_push($values[$key], $oiii2);
//                                        }
//                                    }
//                                        //$values[$o[0]][$oii[0]][$oiii[0]][$iv] = $oiii[$iv];
//                                }
//                            } else if ($iii == count($oii) - 1) {
//                                print_r($oii);
//                                if(count($oii) == 2)
//                                    $values[$key][$keyii] = $oii[1];
//                                else {
//                                    $oii2 = $oii;
//                                    array_shift($oii2);
//                                    array_push($values[$key], $oii2);
//                                }
//                            }
//                        }
//                    } else if ($ii == count($o) - 1) {
//                        if(count($o) == 2)
//                            $values[$key] = $o[1];
//                        else
//                            array_push($values[$key], $o[$ii]);
//                    }
//                }
//            }
//        }
//        $response["values"] = $values;
//        $response["row"] = $columns;
//        return $response;
//}