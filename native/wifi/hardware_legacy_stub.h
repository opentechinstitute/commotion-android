/*
 * This just provides stubs to build against.  The actual functions
 * are provided by a vendor supplied library for interfacing with the
 * wifi hardware.  This build system does build a
 * libhardware_legacy.so, but this is only for linking against, it is
 * not deployed at all, since we expect every Android device to
 * already have libhardware_legacy.so installed.
 */

#include <stdlib.h>
int wifi_load_driver();
int wifi_unload_driver();
int wifi_start_supplicant();
int wifi_stop_supplicant();
int wifi_connect_to_supplicant();
void wifi_close_supplicant_connection();
int wifi_wait_for_event(char *buf, size_t len);
int wifi_command(const char *command, char *reply, size_t *reply_len);
