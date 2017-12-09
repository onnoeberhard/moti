<?php 
	error_reporting(0);
	if (isset($_POST['tag']) && $_POST['tag'] != '') {
		$tag = $_POST['tag'];
		//mysql_connect('localhost', 'actio', '1234'); the roots hehe (bis auf topic ^^)
		mysql_connect('fdb6.runhosting.com', '2002608_moti', 'ahosT134652');
        mysql_select_db("2002608_moti");
		$response = array("tag" => $tag, "success" => 0, "error" => 0);
		if ($tag == 'EXEC') {
			$sql = $_POST['sql'];
			$result = mysql_query($sql);
			if($result)
				$response["success"] = 1;
			else
				$response["error"] = 1;
			$response["sql"] = $sql;
			echo json_encode($response);
		} else if ($tag == 'EXPLODE') {
			$string = $_POST['string'];
			$result = mysql_query($sql);
			$response["values"] = explode("{{;}}", $string);
			$keys = array();
			$values = array();
			for($i = 0; $i < count($response["values"]); $i++) {
				$o = explode("{{:}}", $response["values"][$i]);
				array_push($keys, $o[0]);
				$values[$i] = array();
				for($ii = 0; $ii < count($o); $ii++) {
					if(strpos($o[$ii], "{{,}}") !== false) {
						$oii = explode("{{,}}", $o[$ii]);
						$nextLevel = false;
						for($iii = 0; $iii < count($oii); $iii++) {
							if(strpos($oii[$iii], "{{.}}") !== false) {
								$nextLevel = true;
								$oiii = explode("{{.}}", $oii[$iii]);
								$nextLevel2 = false;
								for($iv = 0; $iv < count($oiii); $iv++) {
									if(strpos($oiii[$iv], "{{|}}") !== false) {
										$nextLevel2 = true;
										$oiv = explode("{{|}}", $oiii[$iv]);
										$values[$i][$oii[0]][$oiii[0]][$oiv[0]] = $oiv;
									} else
										$values[$i][$oii[0]][$oiii[0]][$iv] = $oiii[$iv];
								}
							} else
								$values[$i][$oii[0]][$iii] = $oii[$iii];
						}
					} else {
						array_push($values[$i], $o[$ii]);
					}
				}
			}
			$response["values"] = array_combine($keys, $values);
			$response["success"] = 1;
			echo json_encode($response);
		} else if ($tag == 'INQUIRE') {
			//$messages = get("User", "id", $_POST['uid'], "messages");
			$sql = "SELECT * FROM `User` WHERE `id` = '".$_POST['uid']."'";
			$result = mysql_query($sql);
			$rows = array();
			while($row = mysql_fetch_array($result))
				array_push($rows, $row);
			$messages = $rows[0]['messages'];
			$response['messages'] = $messages;
			if(!isset($_POST['location'])) {
				$sql = "UPDATE `User` SET `location` = '' WHERE `id` = '".$_POST['uid']."'";
				mysql_query($sql);
			} else {
				$loc = explode(",", $_POST['location']);
				$sql = "UPDATE `User` SET `location` = '".$_POST['location']."' WHERE `id` = '".$_POST['uid']."'";
				mysql_query($sql);
				$x1 = deg2rad($loc[0]);
				$y1 = deg2rad($loc[1]);
				//$p1radius = $_POST['radius'];
				$p1radius = $rows[0]['radius'];
				$rids = array();
				$sql = "SELECT * FROM `Request`";
				$result = mysql_query($sql);
				$rows = array();
				while($row = mysql_fetch_array($result))
					array_push($rows, $row);
				for($i = 0; $i < count($rows); $i++) {
					$sql = "SELECT * FROM `User` WHERE `id` = '".$rows[$i]['uid']."'";
					$result = mysql_query($sql);
					$_rows = array();
					while($_row = mysql_fetch_array($result))
						array_push($_rows, $_row);
					$rloc = explode(",", $_rows[0]['location']);
					$x2 = deg2rad($rloc[0]);
					$y2 = deg2rad($rloc[1]);
					$p2radius = $_rows[0]['radius'];
					$r = $p1radius + $p2radius;
					$d = acos(sin($x1) * sin($x2) + cos($x1) * cos($x2) * cos($y2 - $y1)) * 6371000;
					if($d <= $r) {
						$age_min = 0;
						$age_max = 0;
						if($rows[$i]['ages'] != "") {
							$ages = explode(";", $rows[$i]['ages']);
							$age_min = $ages[0];
							$age_max = $ages[1];
						}
						if((isset($_POST['age']) && $_POST['age'] != "" && $_POST['age'] >= $age_min && $_POST['age'] <= $age_max) || $rows[$i]['ages'] == "") {
							if($rows[$i]['gender'] == "a" || !isset($_POST['gender']) || $rows[$i]['gender'] == $_POST['gender'])
								array_push($rids, $rows[$i]['id']);	
						}
					}
					//$response['d'] = $d;
				}
				$response['requests'] = $rids;
				$pids = array();
				$sql = "SELECT * FROM `User` WHERE `location` <> ''";
				$result = mysql_query($sql);
				$rows = array();
				while($row = mysql_fetch_array($result))
					array_push($rows, $row);
				for($i = 0; $i < count($rows); $i++) {
					$rloc = explode(",", $rows[$i]['location']);
					$x2 = deg2rad($rloc[0]);
					$y2 = deg2rad($rloc[1]);
					$p2radius = $rows[$i]['radius'];
					$r = $p1radius + $p2radius;
					$d = acos(sin($x1) * sin($x2) + cos($x1) * cos($x2) * cos($y2 - $y1)) * 6371000;
					if($d <= $r && $rows[$i]['id'] != $_POST['uid']) {
						array_push($pids, $rows[$i]['id']);	
					}
				}
				$response['nearpeople'] = $pids;
			}
			$response["success"] = 1;
			echo json_encode($response);
		} else if ($tag == 'GETMEETINGPOINTS') {
			$mps = array();
			$sql = "SELECT * FROM `MeetingPoint` WHERE `active` = '1'";
			$result = mysql_query($sql);
			$rows = array();
			while($row = mysql_fetch_array($result))
				array_push($rows, $row);
			$bounds = explode(";", $_POST['bounds']);
			$ne = explode(",", $bounds[0]);
			$sw = explode(",", $bounds[1]);
			$min_lat = $sw[0];
			$max_lat = $ne[0];
			$min_lng = $sw[1];
			$max_lng = $ne[1];
			for($i = 0; $i < count($rows); $i++) {
				$loc = explode(",", $rows[$i]["location"]);
				if(($loc[0] > $min_lat) && ($loc[0] < $max_lat) && ($loc[1] > $min_lng) && ($loc[1] < $max_lng))
					array_push($mps, $rows[$i]);
			}
			$response["mps"] = $mps;
			//if($result)
				$response["success"] = 1;
			/*else
				$response["error"] = 1;*/
			echo json_encode($response);
		} else if ($tag == 'MAIL') {
			$body = $_POST['body'];
			$subject = $_POST['subject'];
			$recipient = $_POST['recipient'];
			$result = mail($recipient, $subject, $body, "From: Moti<noreply@moti.web44.net>", "-f noreply@moti.web44.net");
			if($result)
				$response["success"] = 1;
			else
				$response["error"] = 1;
			echo json_encode($response);
		} else if ($tag == 'GET') {
			//echo json_encode(get($_POST['table'], $_POST['id_name'], $_POST['id_value'], $_POST['column']));
			$table = $_POST['table'];
			$id_name = $_POST['id_name'];
			$id_value = $_POST['id_value'];
			$column = $_POST['column'];
			$sql = "SELECT * FROM `".$table."` WHERE `".$id_name."` = '".$id_value."'";
			$result = mysql_query($sql);
			$rows = array();
			while($row = mysql_fetch_array($result))
				array_push($rows, $row);
			$columns = $rows[0];
			if($columns[$column] == NULL) {
				$response["error"] = 1;
			} else {
				$response["success"] = 1;
			}
			$response["name"] = $column;
			$response["value"] = $columns[$column];
			$response["values"] = explode("{{;}}", $columns[$column]);
			$keys = array();
			$values = array();
			for($i = 0; $i < count($response["values"]); $i++) {
				$o = explode("{{:}}", $response["values"][$i]);
				array_push($keys, $o[0]);
				$values[$i] = array();
				for($ii = 0; $ii < count($o); $ii++) {
					if(strpos($o[$ii], "{{,}}") !== false) {
						$oii = explode("{{,}}", $o[$ii]);
						$nextLevel = false;
						for($iii = 0; $iii < count($oii); $iii++) {
							if(strpos($oii[$iii], "{{.}}") !== false) {
								$nextLevel = true;
								$oiii = explode("{{.}}", $oii[$iii]);
								$nextLevel2 = false;
								for($iv = 0; $iv < count($oiii); $iv++) {
									if(strpos($oiii[$iv], "{{|}}") !== false) {
										$nextLevel2 = true;
										$oiv = explode("{{|}}", $oiii[$iv]);
										$values[$i][$oii[0]][$oiii[0]][$oiv[0]] = $oiv;
									} else
										$values[$i][$oii[0]][$oiii[0]][$iv] = $oiii[$iv];
								}
							} else
								$values[$i][$oii[0]][$iii] = $oii[$iii];
						}
					} else {
						array_push($values[$i], $o[$ii]);
					}
				}
			}
			$response["values"] = array_combine($keys, $values);
			$response["row"] = $columns;
			echo json_encode($response);
		} else if ($tag == 'GETALL') {
			$table = $_POST['table'];
			$column = $_POST['column'];
			$id_name = $_POST['id_name'];
			$id_value = $_POST['id_value'];

			$sql = "SELECT * FROM `".$table."`";
			if($id_name != "")
				$sql = "SELECT * FROM `".$table."` WHERE `".$id_name."` = '".$id_value."'";
			$result = mysql_query($sql);
			$rows = array();
			while($row = mysql_fetch_array($result))
				array_push($rows, $row);
			$value = array();
			for($i = 0; $i < count($rows); $i++)
				$__values[$i] = $rows[$i][$column];
			if($rows == NULL || count($rows) == 0) {
				$response["error"] = 1;
			} else {
				$response["success"] = 1;
			}
			$response["rows"] = $rows;
			$response["value"] = $__values;
			$_values = array();
			for($oo = 0; $oo < count($__values); $oo++) {
				$_value = $__values[$oo];
				$value = explode("{{;}}", $_value);
				$keys = array();
				$values = array();
				for($i = 0; $i < count($value); $i++) {
					$o = explode("{{:}}", $value[$i]);
					array_push($keys, $o[0]);
					$values[$i] = array();
					for($ii = 0; $ii < count($o); $ii++) {
						if(strpos($o[$ii], "{{,}}") !== false) {
							$oii = explode("{{,}}", $o[$ii]);
							$nextLevel = false;
							for($iii = 0; $iii < count($oii); $iii++) {
								if(strpos($oii[$iii], "{{.}}") !== false) {
									$nextLevel = true;
									$oiii = explode("{{.}}", $oii[$iii]);
									$nextLevel2 = false;
									for($iv = 0; $iv < count($oiii); $iv++) {
										if(strpos($oiii[$iv], "{{|}}") !== false) {
											$nextLevel2 = true;
											$oiv = explode("{{|}}", $oiii[$iv]);
											$values[$i][$oii[0]][$oiii[0]][$oiv[0]] = $oiv;
										} else
											$values[$i][$oii[0]][$oiii[0]][$iv] = $oiii[$iv];
									}
								} else
									$values[$i][$oii[0]][$iii] = $oii[$iii];
							}
						} else {
							array_push($values[$i], $o[$ii]);
						}
					}
				}
				$_values[$oo] = array_combine($keys, $values);
			}
			$response["values"] = $_values;
			echo json_encode($response);
		} else if ($tag == 'SMARTINUP') {
			$table = $_POST['table'];
			$doit = true;
			$pairs = array();
			for($i = 0; $i < count($_POST)-1; $i++) {
				if($_POST['name_'.$i] != "") {
					$pairs[$i]["name"] = $_POST['name_'.$i];
					$pairs[$i]["value"] = $_POST['value_'.$i];
				}
				if($_POST['name_'.$i] == "ABORT" || $_POST['value_'.$i] == "ABORT")
					$doit = false;
			}
			$sql = "SELECT * FROM `".$table."` WHERE `".$pairs[0]["name"]."` = '".$pairs[0]["value"]."'";
			$result = mysql_query($sql);
			$rows = array();
			while($row = mysql_fetch_array($result))
				array_push($rows, $row);
			$columns = $rows[0];
			$sql = "";
			if($columns["id"] == NULL && $pairs[1]["value"] != "*DELETE*") {
				$response["action"] = "insert";
				$sql = "INSERT INTO `".$table."` (";
				for($i = 0; $i < count($pairs); $i++) {
					$sql .= "`".$pairs[$i]["name"]."`";
					if($i < (count($pairs) - 1))
						$sql .= ", ";
				}
				$sql .= ")VALUES(";
				for($i = 0; $i < count($pairs); $i++) {
					$sql .= "'".$pairs[$i]["value"]."'";
					if($i < (count($pairs) - 1))
						$sql .= ", ";
				}
				$sql .= ")";
			} else if($pairs[1]["value"] != "" && $pairs[1]["value"] != "*DELETE*") {
				$response["action"] = "update";
				$sql = "UPDATE `".$table."` SET ";
				for($i = 1; $i < count($pairs); $i++) {
					$sql .= "`".$pairs[$i]["name"]."` = '".$pairs[$i]["value"]."'";
					if($i < (count($pairs) - 1))
						$sql .= ", ";
				}
				$sql .= " WHERE `".$pairs[0]["name"]."` = '".$pairs[0]["value"]."'";
			} else {
				$response["action"] = "delete";
				$sql = "DELETE FROM `".$table."` WHERE `".$pairs[0]["name"]."` = '".$pairs[0]["value"]."'";
			}
			$result = false;
			if($doit)
				$result = mysql_query($sql);
			if($result !== false)
				$response["success"] = 1;
			else
				$response["error"] = 1;
			$response["sql1"] = $sql;
			$sql = "SELECT * FROM `".$table."` WHERE `".$pairs[0]["name"]."` = '".$pairs[0]["value"]."'";
			$result = mysql_query($sql);
			$rows = array();
			while($row = mysql_fetch_array($result))
				array_push($rows, $row);
			$columns = $rows[0];
			$response["row"] = $columns;
			echo json_encode($response);
		} else if ($tag == 'INUP_DATA') {
			$table = $_POST['table'];
			$id_name = $_POST['id_name'];
			$id_value = $_POST['id_value'];
			$column = $_POST['column'];
			$level1 = $_POST['level1'];
			$level2 = $_POST['level2'];
			$level3 = $_POST['level3'];
			$level4 = $_POST['level4'];
			$value = $_POST['value'];
			$sql = "SELECT * FROM `".$table."` WHERE `".$id_name."` = '".$id_value."'";
			$result = mysql_query($sql);
			$rows = array();
			while($row = mysql_fetch_array($result))
				array_push($rows, $row);
			$columns = $rows[0];
			$data = explode("{{;}}", $columns[$column]);
			$keys = array();
			$values = array();
			for($i = 0; $i < count($data); $i++) {
				$o = explode("{{:}}", $data[$i]);
				array_push($keys, $o[0]);
				$values[$i] = array();
				for($ii = 0; $ii < count($o); $ii++) {
					if(strpos($o[$ii], "{{,}}") !== false) {
						$oii = explode("{{,}}", $o[$ii]);
						$nextLevel = false;
						for($iii = 0; $iii < count($oii); $iii++) {
							if(strpos($oii[$iii], "{{.}}") !== false) {
								$nextLevel = true;
								$oiii = explode("{{.}}", $oii[$iii]);
								$nextLevel2 = false;
								for($iv = 0; $iv < count($oiii); $iv++) {
									if(strpos($oiii[$iv], "{{|}}") !== false) {
										$nextLevel2 = true;
										$oiv = explode("{{|}}", $oiii[$iv]);
										$values[$i][$oii[0]][$oiii[0]][$oiv[0]] = $oiv;
									} else
										$values[$i][$oii[0]][$oiii[0]][$iv] = $oiii[$iv];
								}
							} else
								$values[$i][$oii[0]][$iii] = $oii[$iii];
						}
					} else {
						array_push($values[$i], $o[$ii]);
					}
				}
			}
			$data = array_combine($keys, $values);
			$levels = 4 - (($level1 == "") ? 1 : 0) - (($level2 == "") ? 1 : 0) - (($level3 == "") ? 1 : 0) - (($level4 == "") ? 1 : 0);
			if($levels == 4)
				$data[$level1][$level2][$level3][$level4] = $value;
			if($levels == 3)
				$data[$level1][$level2][$level3] = $value;
			if($levels == 2)
				$data[$level1][$level2] = $value;
			if($levels == 1)
				$data[$level1] = $value;
			if($levels == 0)
				$data = $value;
			$keysI = array_keys($data);
			for($i = 0; $i < count($data); $i++) {
				if(is_array($data[$keysI[$i]])) {
					$keysII = array_keys($data[$keysI[$i]]);
					for($ii = 0; $ii < count($data[$keysI[$i]]); $ii++) {
						if(is_array($data[$keysI[$i]][$keysII[$ii]])) {
							$keysIII = array_keys($data[$keysI[$i]][$keysII[$ii]]);
							for($iii = 0; $iii < count($data[$keysI[$i]][$keysII[$ii]]); $iii++) {
								if(is_array($data[$keysI[$i]][$keysII[$ii]][$keysIII[$iii]])) {
									$keysIV = array_keys($data[$keysI[$i]][$keysII[$ii]][$keysIII[$iii]]);
									for($iv = 0; $iv < count($data[$keysI[$i]][$keysII[$ii]][$keysIII[$iii]]); $iv++) {
										if(is_array($data[$keysI[$i]][$keysII[$ii]][$keysIII[$iii]][$keysIV[$iv]])) {
											$data[$keysI[$i]][$keysII[$ii]][$keysIII[$iii]][$keysIV[$iv]] = 
												implode("{{|}}", $data[$keysI[$i]][$keysII[$ii]][$keysIII[$iii]][$keysIV[$iv]]);
										}
									}
									$data[$keysI[$i]][$keysII[$ii]][$keysIII[$iii]] = implode("{{.}}", $data[$keysI[$i]][$keysII[$ii]][$keysIII[$iii]]);
								}
							}
							$data[$keysI[$i]][$keysII[$ii]] = implode("{{,}}", $data[$keysI[$i]][$keysII[$ii]]);
						}
					}
					$data[$keysI[$i]] = implode("{{:}}", $data[$keysI[$i]]);
				}
			}
			$data = implode("{{;}}", array_filter($data));
			$sql = "UPDATE `".$table."` SET `".$column."` = '".$data."' WHERE `".$id_name."` = '".$id_value."'";
			$result = mysql_query($sql);
			if($result !== false)
				$response["success"] = 1;
			else
				$response["error"] = 1;
			$response["sql1"] = $sql;
			$sql = "SELECT * FROM `".$table."` WHERE `".$id_name."` = '".$id_value."'";
			$result = mysql_query($sql);
			$rows = array();
			while($row = mysql_fetch_array($result))
				array_push($rows, $row);
			$columns = $rows[0];
			$response["row"] = $columns;
			echo json_encode($response);
		} else {
			echo "Invalid Request";
		}
	} else {
		echo "Access Denied";
	} 
	
	function get ($table, $id_name, $id_value, $column) {		
	/*
		$sql = "SELECT * FROM `".$table."` WHERE `".$id_name."` = '".$id_value."'";
		$result = mysql_query($sql);
		$rows = array();
		while($row = mysql_fetch_array($result))
			array_push($rows, $row);
		$columns = $rows[0];
		if($columns[$column] == NULL) {
			$response["error"] = 1;
		} else {
			$response["success"] = 1;
		}
		$response["name"] = $column;
		$response["value"] = $columns[$column];
		//einführen: {{A}}
		//password{{:}}1747b1f98f4a057b8bae03497894c670{{;}}name{{:}}Onno{{;}}created_dt{{:}}20151127160420{{:}}byme:D{{;}}messages{{:}}{{A}}{{,}}2{{,}}hallo{{,}}123456789{{:}}{{A}}{{,}}7{{,}}hallovon7{{,}}987654321{{;}}{{A}}{{:}}hallo{{:}}{{A}}{{,}}q{{,}}w{{:}}mhm{{;}}{{A}}{{:}}ha2{{,}}a2{{:}}ha3{{,}}a3
		if(strpos($o[$ii], "{{;}}")) {
			$response["values"] = explode("{{;}}", $columns[$column]);
			$values = array();
			//6 -> a1{{:}}{{A}}{{,}}v1{{,}}{{A}}{{.}}v21{{.}}v22{{:}}b1{{:}}{{A}}{{,}}n1{{,}}n2{{,}}{{A}}{{.}}n31{{.}}n32
			for($i = 0; $i < count($response["values"]); $i++) {
				$o = explode("{{:}}", $response["values"][$i]);
				//{{A}}{{,}}n1{{,}}n2{{,}}{{A}}{{.}}n31{{.}}n32
				$key = $o[0];
				if($o[0] == "{{A}}")
					$key = $i;
				$values[$key] = array();
				for($ii = 1; $ii < count($o); $ii++) {
					if(strpos($o[$ii], "{{,}}")) {
						$oii = explode("{{,}}", $o[$ii]);
						//{{A}} | n1 | n2 | {{A}}{{.}}n31{{.}}n32
						$keyii = $oii[0];
						if($oii[0] == "{{A}}")
							$keyii = $ii;
						for($iii = 1; $iii < count($oii); $iii++) {
							if(strpos($oii[$iii], "{{.}}")) {
								$oiii = explode("{{.}}", $oii[$iii]);
								$keyiii = $oiii[0];
								if($oiii[0] == "{{A}}")
									$keyiii = $iii;
								for($iv = 1; $iv < count($oiii); $iv++) {
									if(strpos($oiii[$iv], "{{|}}")) {
										$oiv = explode("{{|}}", $oiii[$iv]);
										$values[$o[0]][$oii[0]][$oiii[0]][$oiv[0]] = $oiv;
									} else if ($iv == count($oiii) - 1) {
										if(count($oiii) == 2)
											$values[$key][$keyii][$keyiii] = $oiii[1];
										else {
											//$values[$key][$keyii] = array();
											$oiii2 = $oiii;
											array_shift($oiii2);
											array_push($values[$key], $oiii2);
										}
									}
										//$values[$o[0]][$oii[0]][$oiii[0]][$iv] = $oiii[$iv];
								}
							} else if ($iii == count($oii) - 1) {
								print_r($oii); 
								if(count($oii) == 2)
									$values[$key][$keyii] = $oii[1];
								else {
									$oii2 = $oii;
									array_shift($oii2);
									array_push($values[$key], $oii2);
								}
							}
						}
					} else if ($ii == count($o) - 1) {
						if(count($o) == 2)
							$values[$key] = $o[1];
						else 
							array_push($values[$key], $o[$ii]);
					}
				}
			}
		}
		$response["values"] = $values;
		$response["row"] = $columns;
		return $response;
		*/
	}
?>