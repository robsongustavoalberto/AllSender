package com.allsender.app;

import android.net.Uri;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Estado partilhado entre a Activity, o serviço e o servidor web. */
final class ShareState {
    static final class Item {
        final Uri uri; final String name; final long size;
        Item(Uri uri, String name, long size) { this.uri = uri; this.name = name; this.size = size; }
    }
    static final List<Item> items = Collections.synchronizedList(new ArrayList<Item>());
    static volatile String url; // null = partilha parada
}
