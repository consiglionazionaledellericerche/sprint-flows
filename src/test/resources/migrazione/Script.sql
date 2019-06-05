drop database if exists activiti;
create database activiti;
grant all privileges on database activiti to activiti;

select version();

update act_ru_variable set type_ = 'string' where type_ = 'alfrescoScriptNode';
update act_ru_variable set type_ = 'string' where name_ = 'wfcnr_wfNodeRefCartellaFlusso';
update act_hi_varinst set var_type_ = 'string' where var_type_ = 'alfrescoScriptNode';
update act_hi_varinst set var_type_ = 'string' where name_ = 'wfcnr_wfNodeRefCartellaFlusso';
