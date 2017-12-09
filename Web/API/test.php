<?php
$salt = openssl_random_pseudo_bytes(64);
$pw = "Hallo";
$hash = hash("sha256", $pw . $salt);
$password = $hash . $salt;
echo "salt = " . $salt . "\n";
echo "hash = " . $hash. "\n";
echo hash("sha256", "Hallo" . substr($password, 64, 64)) == substr($password, 0, 64);
//echo $password. "\n";
////sendGCM("", "");
//function sendGCM($uid, $data) {
//    $url = 'https://gcm-http.googleapis.com/gcm/send';
//    $registration_ids = array("eWMBsNnyVpA:APA91bH6ZsZKDGDtKb8x1M19C0xKXJX0t_xPJ-BlES6I9M4aGpErVtmLD3cypx7WjqXC5js78VSgjt_pLGXIDcNQkOxQdbok5PAKTUrA8s5Pckji95V9O0FKsegZYSE9s6RBFLECEHuW");
//    $message = array("message" => "hello", "type" => "m");
//    $fields = array(
//        'registration_ids' => $registration_ids,
////    'to' => "/topics/m",
//        'data' => $message,
//    );
//    define("GOOGLE_API_KEY", "AIzaSyAw_MQhtK_2ca9ce-fTeugYMxiguvwkqNo");
//    $headers = array(
//        'Authorization: key=' . GOOGLE_API_KEY,
//        'Content-Type: application/json'
//    );
//    $ch = curl_init();
//    curl_setopt($ch, CURLOPT_URL, $url);
//    curl_setopt($ch, CURLOPT_POST, true);
//    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
//    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
//    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($fields));
////    curl_exec($ch);
//    $result = curl_exec($ch);
//    if ($result === FALSE) {
//        die('Curl failed: ' . curl_error($ch));
//    }
//    echo $result;
//    curl_close($ch);
//}