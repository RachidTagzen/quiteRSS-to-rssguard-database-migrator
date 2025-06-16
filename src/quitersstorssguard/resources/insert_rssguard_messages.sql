INSERT INTO Messages (
                      author,
                      contents,
                      custom_id,
                      date_created,
                      feed,
                      id,
                      is_deleted,
                      is_pdeleted,
                      is_important,
                      is_read,
                      title,
                      url,
                      account_id
                      )
VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?);
