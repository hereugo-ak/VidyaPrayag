academic_calender:


create table public.academic_calendar (
  id uuid not null,
  school_id uuid not null,
  event_id text not null,
  date character varying(12) not null,
  day character varying(16) not null,
  event_title text not null,
  event_description text null,
  standard text null,
  is_holiday boolean not null default false,
  created_at timestamp without time zone not null,
  constraint academic_calendar_pkey primary key (id),
  constraint academic_calendar_event_id_unique unique (event_id)
) TABLESPACE pg_default;



admission_enquiries:



create table public.admission_enquiries (
  id uuid not null,
  school_id uuid not null,
  student_name text not null,
  parent_name text not null,
  parent_phone text null,
  parent_email text null,
  class_name text not null,
  date character varying(12) not null,
  status character varying(16) not null default 'new'::character varying,
  profile_pic text null,
  source character varying(32) null,
  notes text null,
  assigned_to uuid null,
  converted_at timestamp without time zone null,
  created_at timestamp without time zone not null,
  updated_at timestamp without time zone not null,
  constraint admission_enquiries_pkey primary key (id)
) TABLESPACE pg_default;




announcements:




create table public.announcements (
  id uuid not null,
  school_id uuid not null,
  event_id text not null,
  type character varying(16) not null,
  title text not null,
  sub_title text null,
  description text not null,
  event_image text null,
  date character varying(12) not null,
  synced_to_wa boolean not null default false,
  created_by uuid null,
  created_at timestamp without time zone not null,
  updated_at timestamp without time zone not null,
  constraint announcements_pkey primary key (id),
  constraint announcements_event_id_unique unique (event_id)
) TABLESPACE pg_default;





app_config:


create table public.app_config (
  key text not null,
  value text not null,
  updated_at timestamp without time zone not null,
  constraint app_config_pkey primary key (key)
) TABLESPACE pg_default;




app_users:

create table public.app_users (
  id uuid not null,
  linked_auth_user_id uuid null,
  school_id uuid null,
  role character varying(32) not null default 'parent'::character varying,
  full_name text not null,
  phone character varying(32) null,
  email character varying(255) null,
  password_hash text null,
  profile_pic_url text null,
  language_pref character varying(8) not null default 'hi'::character varying,
  is_phone_verified boolean not null default false,
  is_email_verified boolean not null default false,
  profile_completed boolean not null default false,
  is_active boolean not null default true,
  last_login_at timestamp without time zone null,
  created_at timestamp without time zone not null,
  updated_at timestamp without time zone not null,
  constraint app_users_pkey primary key (id),
  constraint app_users_email_unique unique (email),
  constraint app_users_phone_unique unique (phone)
) TABLESPACE pg_default;





attendance_records:


create table public.attendance_records (
  id uuid not null,
  school_id uuid not null,
  date character varying(12) not null,
  type character varying(16) not null,
  person_id text not null,
  grade text null,
  status character varying(16) not null,
  marked_by uuid null,
  created_at timestamp without time zone not null,
  constraint attendance_records_pkey primary key (id),
  constraint ux_att_records_unique unique (school_id, date, type, person_id)
) TABLESPACE pg_default;



auth_otps:


create table public.auth_otps (
  id uuid not null,
  identifier text not null,
  identifier_type character varying(8) not null,
  purpose character varying(24) not null default 'login'::character varying,
  code_hash text not null,
  code_salt text not null,
  sent_at timestamp without time zone not null,
  first_sent_at timestamp without time zone not null,
  expires_at timestamp without time zone not null,
  resend_count smallint not null default 0,
  attempt_count smallint not null default 0,
  max_attempts smallint not null default 5,
  max_resends smallint not null default 5,
  resend_window_secs integer not null default 3600,
  is_verified boolean not null default false,
  is_locked boolean not null default false,
  verified_at timestamp without time zone null,
  ip_address text null,
  user_agent text null,
  device_id text null,
  delivery_channel character varying(16) null,
  delivery_provider character varying(32) null,
  provider_message_id text null,
  created_at timestamp without time zone not null,
  updated_at timestamp without time zone not null,
  constraint auth_otps_pkey primary key (id),
  constraint ux_auth_otps_identifier_purpose unique (identifier, purpose)
) TABLESPACE pg_default;




children:


create table public.children (
  id uuid not null,
  parent_id uuid not null,
  school_id uuid null,
  child_name text not null,
  date_of_birth character varying(12) null,
  gender character varying(16) null,
  current_grade character varying(32) null,
  interests text not null default '[]'::text,
  profile_pic text null,
  overall_progress double precision not null default 0.0,
  current_level integer not null default 1,
  attendance_status character varying(16) not null default 'PRESENT'::character varying,
  is_active boolean not null default true,
  created_at timestamp without time zone not null,
  updated_at timestamp without time zone not null,
  constraint children_pkey primary key (id)
) TABLESPACE pg_default;




cms_landing_content:


create table public.cms_landing_content (
  key text not null,
  value text not null,
  updated_at timestamp without time zone not null,
  constraint cms_landing_content_pkey primary key (key)
) TABLESPACE pg_default;



exam_results :


create table public.exam_results (
  id uuid not null,
  school_id uuid not null,
  test text not null,
  class_name text not null,
  subject text not null,
  student_id text not null,
  student_name text not null,
  image_url text null,
  attendance character varying(8) not null default '0%'::character varying,
  score character varying(8) not null default ''::character varying,
  status character varying(16) not null default 'Pending'::character varying,
  trend character varying(8) not null default '0%'::character varying,
  created_at timestamp without time zone not null,
  updated_at timestamp without time zone not null,
  constraint exam_results_pkey primary key (id),
  constraint ux_exam_results_unique unique (school_id, test, class_name, subject, student_id)
) TABLESPACE pg_default;





faculty :


create table public.exam_results (
  id uuid not null,
  school_id uuid not null,
  test text not null,
  class_name text not null,
  subject text not null,
  student_id text not null,
  student_name text not null,
  image_url text null,
  attendance character varying(8) not null default '0%'::character varying,
  score character varying(8) not null default ''::character varying,
  status character varying(16) not null default 'Pending'::character varying,
  trend character varying(8) not null default '0%'::character varying,
  created_at timestamp without time zone not null,
  updated_at timestamp without time zone not null,
  constraint exam_results_pkey primary key (id),
  constraint ux_exam_results_unique unique (school_id, test, class_name, subject, student_id)
) TABLESPACE pg_default;





fee_records:


create table public.fee_records (
  id uuid not null,
  parent_id uuid not null,
  child_id uuid null,
  school_id uuid null,
  title text not null,
  description text null,
  amount double precision not null default 0.0,
  currency character varying(8) not null default 'USD'::character varying,
  due_date character varying(12) null,
  status character varying(16) not null default 'DUE'::character varying,
  category character varying(32) not null default 'Tuition'::character varying,
  created_at timestamp without time zone not null,
  updated_at timestamp without time zone not null,
  constraint fee_records_pkey primary key (id)
) TABLESPACE pg_default;






holiday_list:

create table public.fee_records (
  id uuid not null,
  parent_id uuid not null,
  child_id uuid null,
  school_id uuid null,
  title text not null,
  description text null,
  amount double precision not null default 0.0,
  currency character varying(8) not null default 'USD'::character varying,
  due_date character varying(12) null,
  status character varying(16) not null default 'DUE'::character varying,
  category character varying(32) not null default 'Tuition'::character varying,
  created_at timestamp without time zone not null,
  updated_at timestamp without time zone not null,
  constraint fee_records_pkey primary key (id)
) TABLESPACE pg_default;



leave_requests:

create table public.leave_requests (
  id uuid not null,
  school_id uuid not null,
  requester_id uuid null,
  requester_name text not null,
  requester_role character varying(16) not null default 'student'::character varying,
  date_from character varying(12) not null,
  date_to character varying(12) not null,
  reason text not null,
  image_url text null,
  status character varying(16) not null default 'Pending'::character varying,
  actioned_by uuid null,
  actioned_at timestamp without time zone null,
  created_at timestamp without time zone not null,
  updated_at timestamp without time zone not null,
  constraint leave_requests_pkey primary key (id)
) TABLESPACE pg_default;



message_threads:



create table public.message_threads (
  id uuid not null,
  school_id uuid not null,
  owner_user_id uuid not null,
  sender_name text not null,
  sender_role text not null,
  sender_image_url text null,
  icon_name text null,
  last_message text not null default ''::text,
  last_message_at timestamp without time zone not null,
  unread_count integer not null default 0,
  is_read boolean not null default true,
  created_at timestamp without time zone not null,
  updated_at timestamp without time zone not null,
  constraint message_threads_pkey primary key (id)
) TABLESPACE pg_default;





messages:


create table public.messages (
  id uuid not null,
  thread_id uuid not null,
  sender_id uuid null,
  body text not null,
  created_at timestamp without time zone not null,
  constraint messages_pkey primary key (id)
) TABLESPACE pg_default;



otp_delivery_attempts:


create table public.otp_delivery_attempts (
  id uuid not null,
  otp_id uuid null,
  identifier text not null,
  purpose character varying(24) not null,
  attempt_index integer not null,
  provider_name character varying(64) not null,
  channel character varying(16) not null,
  status character varying(16) not null,
  provider_message_id text null,
  http_status integer null,
  latency_ms integer not null default 0,
  reason text null,
  raw_response text null,
  created_at timestamp without time zone not null,
  constraint otp_delivery_attempts_pkey primary key (id)
) TABLESPACE pg_default;




ptm_class_progress:



create table public.ptm_class_progress (
  id uuid not null,
  ptm_event_id uuid not null,
  class_name text not null,
  teacher_name text not null,
  met_count integer not null default 0,
  total_count integer not null default 0,
  updated_at timestamp without time zone not null,
  constraint ptm_class_progress_pkey primary key (id),
  constraint ux_ptm_class_progress_unique unique (ptm_event_id, class_name)
) TABLESPACE pg_default;





ptm_events:


create table public.ptm_events (
  id uuid not null,
  school_id uuid not null,
  title text not null,
  date character varying(12) not null,
  slot text not null,
  expected_parents integer not null default 0,
  checked_in_parents integer not null default 0,
  invites_delivered integer not null default 0,
  read_receipts integer not null default 0,
  turnout integer not null default 0,
  total_met integer not null default 0,
  created_by uuid null,
  created_at timestamp without time zone not null,
  updated_at timestamp without time zone not null,
  constraint ptm_events_pkey primary key (id)
) TABLESPACE pg_default;



school_classes:


create table public.school_classes (
  id uuid not null,
  school_id uuid not null,
  code text not null,
  name text not null,
  sections text not null default '[]'::text,
  created_at timestamp without time zone not null,
  constraint school_classes_pkey primary key (id),
  constraint ux_classes_school_code unique (school_id, code)
) TABLESPACE pg_default;






school_media:



create table public.school_media (
  id uuid not null,
  school_id uuid not null,
  kind character varying(8) not null,
  url text not null,
  position integer not null default 0,
  size_bytes bigint not null default 0,
  uploaded_by uuid null,
  created_at timestamp without time zone not null,
  constraint school_media_pkey primary key (id)
) TABLESPACE pg_default;






school_onboarding_drafts:


create table public.school_onboarding_drafts (
  id uuid not null,
  user_id uuid not null,
  step_type character varying(16) not null,
  key text not null,
  value text not null,
  updated_at timestamp without time zone not null,
  constraint school_onboarding_drafts_pkey primary key (id),
  constraint ux_ob_drafts_user_step_key unique (user_id, step_type, key)
) TABLESPACE pg_default;





school_philosophy:


create table public.school_philosophy (
  school_id uuid not null,
  core_mission text null,
  learning_model text null,
  primary_language text null,
  public_profile boolean not null default true,
  updated_at timestamp without time zone not null,
  constraint school_philosophy_pkey primary key (school_id)
) TABLESPACE pg_default;




school_subjects:


create table public.school_subjects (
  id uuid not null,
  class_id uuid not null,
  sub_name text not null,
  sub_code text not null,
  teacher_assigned text null,
  created_at timestamp without time zone not null,
  constraint school_subjects_pkey primary key (id)
) TABLESPACE pg_default;





schools:

create table public.schools (
  id uuid not null,
  name text not null,
  slug text not null,
  board character varying(32) not null,
  medium character varying(32) not null,
  school_gender character varying(16) not null default 'co_ed'::character varying,
  contact_phone text null,
  contact_email text null,
  principal_name text null,
  principal_phone text null,
  principal_email text null,
  full_address text null,
  city text not null,
  district text not null,
  state text not null default 'Uttar Pradesh'::text,
  pincode text null,
  logo_url text null,
  brand_color text not null default '#2563EB'::text,
  is_active boolean not null default true,
  onboarded_at timestamp without time zone null,
  created_at timestamp without time zone not null,
  updated_at timestamp without time zone not null,
  constraint schools_pkey primary key (id),
  constraint schools_slug_unique unique (slug)
) TABLESPACE pg_default;





storage_metrics:


create table public.storage_metrics (
  school_id uuid not null,
  total_storage text not null default '10 GB'::text,
  storage_used text not null default '0 B'::text,
  bytes_used bigint not null default 0,
  updated_at timestamp without time zone not null,
  constraint storage_metrics_pkey primary key (school_id)
) TABLESPACE pg_default;





students:


create table public.students (
  id uuid not null,
  school_id uuid not null,
  student_code text not null,
  full_name text not null,
  class_name text not null,
  section text not null default 'A'::text,
  roll_number text not null,
  profile_photo_url text null,
  is_active boolean not null default true,
  created_at timestamp without time zone not null,
  constraint students_pkey primary key (id),
  constraint students_student_code_unique unique (student_code)
) TABLESPACE pg_default;




user_sessions:


create table public.user_sessions (
  id uuid not null,
  user_id uuid not null,
  refresh_token_hash text not null,
  device_id text null,
  platform character varying(16) null,
  ip_address text null,
  user_agent text null,
  issued_at timestamp without time zone not null,
  expires_at timestamp without time zone not null,
  revoked_at timestamp without time zone null,
  last_used_at timestamp without time zone null,
  created_at timestamp without time zone not null,
  constraint user_sessions_pkey primary key (id),
  constraint user_sessions_refresh_token_hash_unique unique (refresh_token_hash)
) TABLESPACE pg_default;





users:

create table public.users (
  id uuid not null,
  name character varying(255) not null,
  contact character varying(255) not null,
  password character varying(255) not null,
  role character varying(50) not null,
  email character varying(255) null,
  phone character varying(50) null,
  password_hash character varying(255) null,
  is_phone_verified boolean not null default false,
  is_email_verified boolean not null default false,
  constraint users_pkey primary key (id),
  constraint users_contact_unique unique (contact)
) TABLESPACE pg_default;




whatsapp_logs:

create table public.whatsapp_logs (
  id uuid not null,
  school_id uuid not null,
  announcement_id text not null,
  job_id text not null,
  phone text not null,
  status character varying(16) not null default 'QUEUED'::character varying,
  provider_message_id text null,
  error_message text null,
  created_at timestamp without time zone not null,
  constraint whatsapp_logs_pkey primary key (id)
) TABLESPACE pg_default;
