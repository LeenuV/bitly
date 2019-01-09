/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nic.bit;

/**
 *
 * @author nitin
 */
class Constants {
	public static final String MDC_IP = "IP";
	public static final String MDC_ID = "ID";

	public static final int DEF_CONNECTION_TIMEOUT = 10000; // millis
	public static final int DEF_READ_TIMEOUT = 30000; // millis
	public static final int DEF_CHECK_CACHE_EXPIRE = 60000; // millis
	public static final int DEF_WHITELIST_RELOAD = 10000; // millis

	public static final int MIN_URL_LENGTH = 12;
	public static final int KEY_SPACE = 6;
	public static final int MAX_COLLISION = 5;
}
