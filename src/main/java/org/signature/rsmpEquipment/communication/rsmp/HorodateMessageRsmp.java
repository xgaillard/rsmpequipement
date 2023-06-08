package org.signature.rsmpEquipment.communication.rsmp;

import java.time.LocalDateTime;

import org.signature.rsmpEquipment.communication.rsmp.json.IdentifiantMessage;

/**
 * Association d'un message RSMP et de sa date d'émission
 * 
 * @author SDARIZCUREN
 *
 */
public class HorodateMessageRsmp {
	protected final IdentifiantMessage _message;
	protected final LocalDateTime _horodate;
	
	protected HorodateMessageRsmp(IdentifiantMessage pMsg, LocalDateTime pHorodate) {
		_message = pMsg;
		_horodate = pHorodate; 
	}
}
